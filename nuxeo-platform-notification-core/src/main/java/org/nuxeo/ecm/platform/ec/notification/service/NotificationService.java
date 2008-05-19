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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.platform.ec.notification.UserSubscription;
import org.nuxeo.ecm.platform.ec.placeful.Annotation;
import org.nuxeo.ecm.platform.ec.placeful.ejb.interfaces.EJBPlacefulService;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public class NotificationService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.ec.notification.service.NotificationService");

    private static final Log log = LogFactory.getLog(NotificationService.class);

    private static final String SUBSCRIPTION_NAME = "UserSubscription";

    private static final Map<String, URL> TEMPLATES_MAP = new HashMap<String, URL>();

    private GeneralSettingsDescriptor generalSettings;

    private NotificationRegistry notificationRegistry;


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

    private static List<String> getNames(List<NotificationEventDescriptor> events) {
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
                            getNames(notifDesc.getEvents()) );
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

    /**
     * @return the notificationRegistry.
     */
    public NotificationRegistry getNotificationRegistry() {
        return notificationRegistry;
    }

    public static List<String> getSubscribers(String notification, String docId)
            throws ClientException {
        PlacefulService service = NotificationServiceHelper.getPlacefulService();
        String className = service.getAnnotationRegistry().get(
                SUBSCRIPTION_NAME);
        // Class klass =
        // Thread.currentThread().getContextClassLoader().loadClass(className);
        String shortClassName = className.substring(className.lastIndexOf('.') + 1);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("notification", notification);
        paramMap.put("docId", docId);

        EJBPlacefulService serviceBean = NotificationServiceHelper
                .getPlacefulServiceBean();
        List<Annotation> subscriptions = new ArrayList<Annotation>();
        try {
            subscriptions = serviceBean.getAnnotationListByParamMap(paramMap,
                    shortClassName);
        } catch (ClassNotFoundException e) {
            log.error(e);
        }
        List<String> subscribers = new ArrayList<String>();
        for (Object obj : subscriptions) {
            UserSubscription subscription = (UserSubscription) obj;
            subscribers.add(subscription.getUserId());
        }
        return subscribers;
    }

    public static List<String> getSubscriptionsForUserOnDocument(String username,
            String docId) throws ClassNotFoundException, ClientException {
        PlacefulService service = NotificationServiceHelper.getPlacefulService();
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
        EJBPlacefulService serviceBean = NotificationServiceHelper
                .getPlacefulServiceBean();

        List<Annotation> subscriptions = serviceBean.getAnnotationListByParamMap(paramMap,
                shortClassName);
        List<String> subscribers = new ArrayList<String>();
        for (Object obj : subscriptions) {
            UserSubscription subscription = (UserSubscription) obj;
            subscribers.add(subscription.getNotification());
        }
        return subscribers;
    }

    public static void addSubscription(String username, String notification,
            DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName) throws ClientException {

        EJBPlacefulService serviceBean = NotificationServiceHelper
                .getPlacefulServiceBean();
        UserSubscription subscription = new UserSubscription(notification,
                username, doc.getId());
        serviceBean.setAnnotation(subscription);

        //send event for email if necessary
        if (sendConfirmationEmail){
            raiseConfirmationEvent(principal, doc, username, notificationName);
        }
    }

    protected static DocumentMessageProducer getMessageProducer() throws Exception {
        return Framework.getService(DocumentMessageProducer.class);
    }

    private static void raiseConfirmationEvent(NuxeoPrincipal principal,
            DocumentModel doc, String username, String notification) {
        // Default category
        String category = DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY;

        Map<String, Serializable> options = new HashMap<String, Serializable>();

        // Name of the current repository
        options.put(CoreEventConstants.REPOSITORY_NAME, doc.getRepositoryName());

        // Add the session ID
        options.put(CoreEventConstants.SESSION_ID, doc.getSessionId());

        //options for confirmation email
        options.put("recipients", username);
        options.put("notifName", notification);

        CoreEvent event = new CoreEventImpl(
                DocumentEventTypes.SUBSCRIPTION_ASSIGNED, doc, options,
                principal, category, null);

        //CoreEventListenerService service = NXCore.getCoreEventListenerService();

        DocumentMessage msg = new DocumentMessageImpl(doc, event);

        DocumentMessageProducer producer=null;

        try {
            producer = getMessageProducer();
        } catch (Exception e) {
            log.error("Unable to get MessageProducer : ",e);
        }

        if (producer != null) {
            log.debug("Send JMS message for for event="
                    + DocumentEventTypes.SUBSCRIPTION_ASSIGNED);
            producer.produce(msg);
        } else {
            log.error("Impossible to notify core events !");
        }
    }

    public static void removeSubscription(String username, String notification,
            String docId) throws ClientException {
        EJBPlacefulService serviceBean = NotificationServiceHelper
                .getPlacefulServiceBean();
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("userId", username);
        paramMap.put("docId", docId);
        paramMap.put("notification", notification);

        try {
            serviceBean.removeAnnotationListByParamMap(paramMap, SUBSCRIPTION_NAME);
            /*
            List<Annotation> subscriptions = serviceBean
                    .getAnnotationListByParamMap(paramMap, SUBSCRIPTION_NAME);
            if (subscriptions != null && subscriptions.size() > 0) {
                for (Annotation subscription : subscriptions) {
                    if (subscription != null) {
                        serviceBean.removeAnnotation(subscription);
                    }
                }
            }*/
        } catch (ClassNotFoundException e) {
            log.debug("Exception occured getting the annotation for removal : ",e);
        }
    }

    public static List<String> getUsersSubscribedToNotificationOnDocument(
            String notification, String docId) throws ClientException {
        EJBPlacefulService serviceBean = NotificationServiceHelper
                .getPlacefulServiceBean();
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("docId", docId);
        paramMap.put("notification", notification);
        List<String> subscribers = new ArrayList<String>();
        try {
            List<Annotation> subscriptions = serviceBean
                    .getAnnotationListByParamMap(paramMap, SUBSCRIPTION_NAME);
            for (Annotation annoSubscription : subscriptions) {
                UserSubscription subscription = (UserSubscription) annoSubscription;
                subscribers.add(subscription.getUserId());
            }
        } catch (ClassNotFoundException e) {
            throw new ClientException(e.getMessage());
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
            if (notification.getName().equals(selectedNotification)){
                return notification;
            }
        }
        return null;
    }

}
