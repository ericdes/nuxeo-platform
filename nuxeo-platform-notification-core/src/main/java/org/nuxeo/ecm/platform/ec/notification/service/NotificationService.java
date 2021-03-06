/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification.service;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.UserSubscription;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.placeful.Annotation;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public class NotificationService extends DefaultComponent implements
        NotificationManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.ec.notification.service.NotificationService");

    private static final Log log = LogFactory.getLog(NotificationService.class);

    private EmailHelper emailHelper= new EmailHelper();

    private static final String SUBSCRIPTION_NAME = "UserSubscription";

    // FIXME: performance issue when putting URLs in a Map.
    private static final Map<String, URL> TEMPLATES_MAP = new HashMap<String, URL>();

    private GeneralSettingsDescriptor generalSettings;

    private NotificationRegistry notificationRegistry;

    private DocumentViewCodecManager docLocator;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(NotificationManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        notificationRegistry = new NotificationRegistryImpl();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        notificationRegistry.clear();
        notificationRegistry = null;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        log.info("Registering notification extention");
        String xp = extension.getExtensionPoint();
        if ("notifications".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                try {
                    NotificationDescriptor notifDesc = (NotificationDescriptor) contrib;
                    notificationRegistry.registerNotification(notifDesc,
                            getNames(notifDesc.getEvents()));
                } catch (Exception e) {
                    log.error(e);
                }
            }
        } else if ("templates".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                try {
                    TemplateDescriptor templateDescriptor = (TemplateDescriptor) contrib;
                    templateDescriptor.setContext(extension.getContext());
                    registerTemplate(templateDescriptor);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        } else if ("generalSettings".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                try {
                    generalSettings = (GeneralSettingsDescriptor) contrib;
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

    private static List<String> getNames(
            List<NotificationEventDescriptor> events) {
        List<String> eventNames = new ArrayList<String>();
        for (NotificationEventDescriptor descriptor : events) {
            eventNames.add(descriptor.name);
        }
        return eventNames;
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        String xp = extension.getExtensionPoint();
        if ("notifications".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                try {
                    NotificationDescriptor notifDesc = (NotificationDescriptor) contrib;
                    notificationRegistry.unregisterNotification(notifDesc,
                            getNames(notifDesc.getEvents()));
                } catch (Exception e) {
                    log.error(e);
                }
            }
        } else if ("templates".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                try {
                    TemplateDescriptor templateDescriptor = (TemplateDescriptor) contrib;
                    templateDescriptor.setContext(extension.getContext());
                    unregisterTemplate(templateDescriptor);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

    public NotificationRegistry getNotificationRegistry() {
        return notificationRegistry;
    }

    public List<String> getSubscribers(String notification, String docId)
            throws ClientException {
        PlacefulService service;
        try {
            service = NotificationServiceHelper.getPlacefulService();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        String className = service.getAnnotationRegistry().get(
                SUBSCRIPTION_NAME);
        // Class klass =
        // Thread.currentThread().getContextClassLoader().loadClass(className);
        String shortClassName = className.substring(className.lastIndexOf('.') + 1);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("notification", notification);
        paramMap.put("docId", docId);

        PlacefulService serviceBean = NotificationServiceHelper.getPlacefulServiceBean();
        List<Annotation> subscriptions = new ArrayList<Annotation>();
        subscriptions = serviceBean.getAnnotationListByParamMap(paramMap,
                shortClassName);
        List<String> subscribers = new ArrayList<String>();
        for (Object obj : subscriptions) {
            UserSubscription subscription = (UserSubscription) obj;
            subscribers.add(subscription.getUserId());
        }
        return subscribers;
    }

    public List<String> getSubscriptionsForUserOnDocument(String username,
            String docId) throws ClassNotFoundException, ClientException {
        PlacefulService service;
        try {
            service = NotificationServiceHelper.getPlacefulService();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        String className = service.getAnnotationRegistry().get(
                SUBSCRIPTION_NAME);
        // Class klass =
        // Thread.currentThread().getContextClassLoader().loadClass(className);
        String shortClassName = className.substring(className.lastIndexOf('.') + 1);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        if (username != null) {
            paramMap.put("userId", username);
        }
        if (docId != null) {
            paramMap.put("docId", docId);
        }
        PlacefulService serviceBean = NotificationServiceHelper.getPlacefulServiceBean();

        List<Annotation> subscriptions = serviceBean.getAnnotationListByParamMap(
                paramMap, shortClassName);
        List<String> subscribers = new ArrayList<String>();
        for (Object obj : subscriptions) {
            UserSubscription subscription = (UserSubscription) obj;
            subscribers.add(subscription.getNotification());
        }
        return subscribers;
    }

    public void addSubscription(String username, String notification,
            DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName)
            throws ClientException {

        PlacefulService serviceBean = NotificationServiceHelper.getPlacefulServiceBean();
        UserSubscription subscription = new UserSubscription(notification,
                username, doc.getId());
        serviceBean.setAnnotation(subscription);

        // send event for email if necessary
        if (sendConfirmationEmail) {
            raiseConfirmationEvent(principal, doc, username, notificationName);
        }
    }

    protected EventProducer producer;

    protected void doFireEvent(Event event) throws ClientException {
        if (producer == null) {
            try {
                producer = Framework.getService(EventProducer.class);
            } catch (Exception e) {
                throw new ClientRuntimeException(
                        "Unable to get MessageProducer : ", e);
            }
        }
        producer.fireEvent(event);
    }

    private void raiseConfirmationEvent(NuxeoPrincipal principal,
            DocumentModel doc, String username, String notification) {

        Map<String, Serializable> options = new HashMap<String, Serializable>();

        // Name of the current repository
        options.put(CoreEventConstants.REPOSITORY_NAME, doc.getRepositoryName());

        // Add the session ID
        options.put(CoreEventConstants.SESSION_ID, doc.getSessionId());

        // options for confirmation email
        options.put("recipients", username);
        options.put("notifName", notification);

        CoreSession session = CoreInstance.getInstance().getSession(
                doc.getSessionId());
        DocumentEventContext ctx = new DocumentEventContext(session, principal,
                doc);
        ctx.setCategory(DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY);
        ctx.setProperties(options);
        Event event = ctx.newEvent(DocumentEventTypes.SUBSCRIPTION_ASSIGNED);

        try {
            doFireEvent(event);
        } catch (ClientException e) {
            throw new ClientRuntimeException("Cannot fire event " + event, e);
        }
    }

    public void removeSubscription(String username, String notification,
            String docId) throws ClientException {
        PlacefulService serviceBean = NotificationServiceHelper.getPlacefulServiceBean();
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("userId", username);
        paramMap.put("docId", docId);
        paramMap.put("notification", notification);

        serviceBean.removeAnnotationListByParamMap(paramMap,
                    SUBSCRIPTION_NAME);
        /*
        * List<Annotation> subscriptions = serviceBean
        * .getAnnotationListByParamMap(paramMap, SUBSCRIPTION_NAME); if
        * (subscriptions != null && subscriptions.size() > 0) { for
        * (Annotation subscription : subscriptions) { if (subscription !=
        * null) { serviceBean.removeAnnotation(subscription); } } }
        */
    }

    public List<String> getUsersSubscribedToNotificationOnDocument(
            String notification, String docId) throws ClientException {
        PlacefulService serviceBean = NotificationServiceHelper.getPlacefulServiceBean();
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("docId", docId);
        paramMap.put("notification", notification);
        List<String> subscribers = new ArrayList<String>();
        List<Annotation> subscriptions = serviceBean.getAnnotationListByParamMap(
                paramMap, SUBSCRIPTION_NAME);
        for (Annotation annoSubscription : subscriptions) {
            UserSubscription subscription = (UserSubscription) annoSubscription;
            subscribers.add(subscription.getUserId());
        }
        return subscribers;
    }

    private static void registerTemplate(TemplateDescriptor td) {
        if (td.src != null && td.src.length() > 0) {
            URL url = td.getContext().getResource(td.src);
            TEMPLATES_MAP.put(td.name, url);
        }
    }

    private static void unregisterTemplate(TemplateDescriptor td) {
        if (td.name != null) {
            TEMPLATES_MAP.remove(td.name);
        }
    }

    public static URL getTemplateURL(String name) {
        return TEMPLATES_MAP.get(name);
    }

    public String getServerUrlPrefix() {
        return generalSettings.getServerPrefix();
    }

    public String getEMailSubjectPrefix() {
        return generalSettings.getEMailSubjectPrefix();
    }

    public String getMailSessionJndiName() {
        return generalSettings.getMailSessionJndiName();
    }

    public Notification getNotificationByName(String selectedNotification) {
        List<Notification> listNotif = notificationRegistry.getNotifications();
        for (Notification notification : listNotif) {
            if (notification.getName().equals(selectedNotification)) {
                return notification;
            }
        }
        return null;
    }

    public void sendNotification(String notificationName,
            Map<String, Object> infoMap, String userPrincipal)
            throws ClientException {

        Notification notif = getNotificationByName(notificationName);

        NuxeoPrincipal recepient = NotificationServiceHelper.getUsersService().getPrincipal(
                userPrincipal);
        // XXX hack, principals have only one model
        DataModel model = recepient.getModel().getDataModels().values().iterator().next();
        String email = (String) model.getData("email");
        String mailTemplate = notif.getTemplate();

        infoMap.put("mail.to", email);

        String authorUsername = (String) infoMap.get("author");

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(
                    authorUsername);
            infoMap.put("principalAuthor", author);
        }

        // mail.put("doc", docMessage); - should be already there

        String subject = notif.getSubject() == null ? "Notification"
                : notif.getSubject();
        if (notif.getSubjectTemplate() != null) {
            subject = notif.getSubjectTemplate();
        }

        subject = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix()
                + subject;

        infoMap.put("subject", subject);
        infoMap.put("template", mailTemplate);

        try {
            emailHelper.sendmail(infoMap);
        } catch (Exception e) {
            throw new ClientException("Failed to send notification email ", e);
        }
    }

    public void sendDocumentByMail(DocumentModel doc,
            String freemarkerTemplateName, String subject, String comment,
            NuxeoPrincipal sender, List<String> sendTo) {
        Map<String, Object> infoMap = new HashMap<String, Object>();
        infoMap.put("document", doc);
        infoMap.put("subject", subject);
        infoMap.put("comment", comment);
        infoMap.put("sender", sender);

        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        DocumentView docView = new DocumentViewImpl(docLoc);
        docView.setViewId("view_documents");
        infoMap.put(
                "docUrl",
                getDocLocator().getUrlFromDocumentView(
                        docView,
                        true,
                        NotificationServiceHelper.getNotificationService().getServerUrlPrefix()));

        if (freemarkerTemplateName == null) {
            freemarkerTemplateName = "defaultNotifTemplate";
        }
        infoMap.put("template", freemarkerTemplateName);

        for (String to : sendTo) {
            infoMap.put("mail.to", to);
            try {
                emailHelper.sendmail(infoMap);
            } catch (Exception e) {
                log.debug("Failed to send notification email " + e);
            }
        }
    }

    private DocumentViewCodecManager getDocLocator() {
        if (docLocator == null) {
            try {
                docLocator = Framework.getService(DocumentViewCodecManager.class);
            } catch (Exception e) {
                log.info("Could not get service for document view manager");
            }
        }

        return docLocator;
    }

    public List<Notification> getNotificationsForSubscriptions(String parentType) {
        return notificationRegistry.getNotificationsForSubscriptions(parentType);
    }

    public List<Notification> getNotificationsForEvents(String eventId) {
        return notificationRegistry.getNotificationsForEvent(eventId);
    }

    public EmailHelper getEmailHelper() {
        return emailHelper;
    }

    public void setEmailHelper(EmailHelper emailHelper) {
        this.emailHelper = emailHelper;
    }

}
