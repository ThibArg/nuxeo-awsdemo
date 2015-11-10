/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
/*
 * WARNING WARNING WARNING
 * 
 *  About all is hard coded, and/orcopy/paste form other plug-ins
 *  The goals is just to quickly create some data, not to build the state-of-the-art example :->
 */

package org.awsdemo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WARNING: This code is not thread safe
 */
@Operation(id = CreateDemoDataOp.ID, category = Constants.CAT_SERVICES, label = "CreateDemoDataOp", description = "")
public class CreateDemoDataOp {

    public static final String ID = "CreateDemoDataOp";

    private static final Log log = LogFactory.getLog(CreateDemoDataOp.class);

    protected static final int COMMIT_MODULO = 50;

    protected static final String[] PARTICIPANTS = { "William Smith",
            "Noelle Peter", "Amanda Bupa", "Andrew Cadabe", "Alvin Calina",
            "Alyson Mateut", "Robert Deji", "Erwin Disume", "Brandie Jemarada",
            "Beverley Tele" };

    protected static final int PARTICIPANTS_MAX = PARTICIPANTS.length - 1;

    protected static final String[] USERS = { "john", "john", "john", "jim",
            "kate", "kate" };

    protected static final int USERS_MAX = USERS.length - 1;

    protected static final String[] MODIF_USERS = { "john", "john", "john",
            "jim", "kate", "kate", "kate", "kate", "external1", "external2",
            "user1", "user2" };

    protected static final int MODIF_USERS_MAX = MODIF_USERS.length - 1;

    protected static final String[] TOPICS = { "EC2", "EC2", "EC2", "EC2",
            "EC2", "S3", "S3", "S3", "Elastic Transcoder" };

    protected static final int TOPICS_MAX = TOPICS.length - 1;;

    protected static final String[] SUBTOPICS = { "Buckets", "Secure Keys",
            "Notifications", "Notifications", "Notifications", "Notifications",
            "Scaling", "Scaling", "Scaling" };

    protected static final int SUBTOPICS_MAX = SUBTOPICS.length - 1;

    protected static final String[] MORETAGS = { "Mobile", "Scale", "Scale",
            "Scale", "Cloud", "Cloud", "Storage" };

    protected static final int MORETAGS_MAX = MORETAGS.length - 1;

    protected static final String[] RATING = { "*", "* *", "* * *", "* * * *",
            "* * * *", "* * * *", "* * * *", "* * * * *", "* * * * *",
            "* * * * *", "* * * * *" };

    protected static final int RATING_MAX = RATING.length - 1;

    protected static final String[] PRODUCTVERSION = { "0.1", "1.0", "1.1",
            "1.5", "1.5", "1.5", "2.0" };

    protected static final int PRODUCTVERSION_MAX = PRODUCTVERSION.length - 1;

    protected HashMap<String, ArrayList<String>> URL_RELATION;

    // WARNING: NO LIMITS...
    protected HashMap<String, String> CACHED_HTML;
    
    protected static ArrayList<String> ALL_URLS;
    
    protected static int ALL_URLS_MAX;

    protected long todayAsMS;

    protected int count = 0;

    protected String parentPath;

    protected DateFormat _yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected Calendar _today = Calendar.getInstance();

    protected String[] contrevenants;

    protected int contrevenants_max;
    
    HashMap<String, DocumentModel> SECTIONS;

    @Context
    protected CoreSession session;
    
    @Context
    protected TagService tagService;
    
    @Context
    protected DocumentRelationManager relationsManager;

    @Param(name = "howMany", required = false, values = { "30" })
    protected long howMany;

    @OperationMethod
    public void run(DocumentModel inDoc) throws IOException, DocumentException, LifeCycleException {

        log.warn("Creating " + howMany + " Articles...");
        
        SECTIONS = new HashMap<String, DocumentModel>();
        for(String oneTopic : TOPICS) {
            String nxql = "SELECT * FROM Section WHERE dc:title = '" + oneTopic + "'";
            DocumentModelList docs = session.query(nxql);
            if(docs.size() == 0) {
                throw new ClientException("Cannot fin a section named '" + oneTopic + "'");
            }
            SECTIONS.put(oneTopic, docs.get(0));
        }

        CACHED_HTML = new HashMap<String, String>();
        ALL_URLS = new ArrayList<String>();
        URL_RELATION = new HashMap<String, ArrayList<String>>();
        ArrayList<String> arr = new ArrayList<String>();
        arr.add("http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/get-set-up-for-amazon-ec2.html");
        arr.add("http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-launch-instance_linux.html");
        arr.add("http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-add-volume-to-instance.html");
        arr.add("http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-clean-up-your-instance.html");
        arr.add("http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-best-practices.html");
        arr.add("http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ComponentsAMIs.html");
        arr.add("http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/virtualization_types.html");
        arr.add("http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-reboot.html");
        URL_RELATION.put("EC2", arr);
        ALL_URLS.addAll(arr);

        arr = new ArrayList<String>();
        arr.add("http://docs.aws.amazon.com/AmazonS3/latest/gsg/AmazonS3Basics.html");
        arr.add("http://docs.aws.amazon.com/AmazonS3/latest/gsg/PuttingAnObjectInABucket.html");
        arr.add("http://docs.aws.amazon.com/AmazonS3/latest/gsg/PuttingAnObjectInABucket.html");
        arr.add("http://docs.aws.amazon.com/AmazonS3/latest/gsg/OpeningAnObject.html");
        arr.add("http://docs.aws.amazon.com/AmazonS3/latest/gsg/S3-gsg-AdvancedAmazonS3Features.html");
        URL_RELATION.put("S3", arr);
        ALL_URLS.addAll(arr);

        arr = new ArrayList<String>();
        arr.add("http://docs.aws.amazon.com/elastictranscoder/latest/developerguide/introduction.html");
        arr.add("http://docs.aws.amazon.com/elastictranscoder/latest/developerguide/accessing.html");
        arr.add("http://docs.aws.amazon.com/elastictranscoder/latest/developerguide/limits.html");
        URL_RELATION.put("Elastic Transcoder", arr);
        ALL_URLS.addAll(arr);
        ALL_URLS_MAX = ALL_URLS.size() - 1;

        todayAsMS = Calendar.getInstance().getTimeInMillis();

        parentPath = inDoc.getPathAsString();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        howMany = howMany <= 0 ? 30 : howMany;
        count = 0;
        for (int i = 1; i <= howMany; i++) {
            createOneArticle();

            count += 1;
            if ((count % 50) == 0) {
                log.warn("Created: " + count + "/" + howMany);
            }
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("Created: " + count + "/" + howMany);

    }

    protected String getHtmlContent(String inUrl) throws IOException {

        String html = CACHED_HTML.get(inUrl);

        if (html != null) {
            return html;
        }

        URL oracle = new URL(inUrl);
        URLConnection yc = oracle.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            html += inputLine + "\n";
        }
        in.close();

        CACHED_HTML.put(inUrl, html);
        return html;

    }

    /*
     * This is _not_ the state of the art, for sure :->
     */
    protected String getTitleFromHtml(String inHtml, String inDefaultPrefix) {

        String title = null;

        int p = inHtml.indexOf("<title>");
        if (p > 0) {
            int p2 = inHtml.indexOf("</title>");
            if (p2 > 0) {
                title = inHtml.substring(p, p2).replace("<title>", "");
            }
        }
        if (title == null) {
            UIDSequencer svc = Framework.getService(UIDSequencer.class);
            int next = svc.getNext("ARTICLE");
            title = inDefaultPrefix + " (" + next + ")";
        }

        return title;
    }

    protected void createOneArticle() throws IOException, DocumentException, LifeCycleException {

        DocumentModel doc;

        String userModif = USERS[RandomValues.randomInt(0, USERS_MAX)];
        String topic = TOPICS[RandomValues.randomInt(0, TOPICS_MAX)];
        String subtopic = SUBTOPICS[RandomValues.randomInt(0, SUBTOPICS_MAX)];
        int anInt = RandomValues.randomInt(0, 4);
        HashMap<String, String> map = new HashMap<String, String>();
        do {
            String p = PARTICIPANTS[RandomValues.randomInt(0, PARTICIPANTS_MAX)];
            map.put(p, p);
        } while (map.size() < anInt);
        String[] participants = new String[anInt];
        map.keySet().toArray(participants);
        String rating = RATING[RandomValues.randomInt(0, RATING_MAX)];
        String productVersion = PRODUCTVERSION[RandomValues.randomInt(0,
                PRODUCTVERSION_MAX)];

        anInt = RandomValues.randomInt(1, 3);
        String[] relations = new String[anInt];
        // Yes, possibly we'll have the same
        for(int i = 0; i < anInt; ++i) {
            relations[i] = ALL_URLS.get(RandomValues.randomInt(0, ALL_URLS_MAX));
        }

        String html = getHtmlContent(relations[0]);
        String title = getTitleFromHtml(html, topic + " - " + subtopic);

        // Create the model
        doc = session.createDocumentModel(parentPath, title, "Article");

        // Setup values
        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("article:participants", participants);
        doc.setPropertyValue("article:product_version",
                PRODUCTVERSION[RandomValues.randomInt(0, PRODUCTVERSION_MAX)]);
        doc.setPropertyValue("article:rating_str",
                RATING[RandomValues.randomInt(0, RATING_MAX)]);
        doc.setPropertyValue("article:subtopic", subtopic);
        doc.setPropertyValue("article:topic", topic);
        doc.setPropertyValue("note:mime_type", "text/html");
        doc.setPropertyValue("note:note", html);
        

        // =========================================== dublincore
        doc.setPropertyValue("dc:creator", USERS[RandomValues.randomInt(0, USERS_MAX)]);
        doc.setPropertyValue("dc:lastContributor", userModif);
        Calendar created = RandomValues.buildDate(null, 0, 30, true);
        doc.setPropertyValue("dc:created", created);
        doc.setPropertyValue("dc:modified", created);
        // We don't setup contributors list (no time)

        doc = session.createDocument(doc);
        saveTheArticle(doc);
        
        // Tags
        tagService.tag(session, doc.getId(), topic, userModif);
        tagService.tag(session, doc.getId(), subtopic, userModif);
        String aTag = MORETAGS[RandomValues.randomInt(0, MORETAGS_MAX)];
        tagService.tag(session, doc.getId(), aTag, userModif);
        
        // Relations
        for(String oneUrl : relations) {
            Node object = new ResourceImpl(oneUrl);
            relationsManager.addRelation(session, doc, object, "http://purl.org/dc/terms/IsBasedOn", false);
        }

        // Now, change the lifecycle state and publish
        if(RandomValues.randomInt(1, 100) > 15) {
            customSetCurrentLifecycleState(doc, "approved");
            VersioningOption vo = VersioningOption.MINOR;
            doc.putContextData(VersioningService.VERSIONING_OPTION, vo);
            doc = session.saveDocument(doc);
            // We must re-add the things for a proxy...
            DocumentModel proxy = session.publishDocument(doc, SECTIONS.get(topic));
            for(String oneUrl : relations) {
                Node object = new ResourceImpl(oneUrl);
                relationsManager.addRelation(session, proxy, object, "http://purl.org/dc/terms/IsBasedOn", false);
            }
            tagService.tag(session, proxy.getId(), topic, userModif);
            tagService.tag(session, proxy.getId(), subtopic, userModif);
            tagService.tag(session, proxy.getId(), aTag, userModif);
        }
    }

    protected void saveTheArticle(DocumentModel inDoc) {

        // Disable dublincore
        inDoc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                true);

        session.saveDocument(inDoc);

        if ((count % COMMIT_MODULO) == 0) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

    }

    /*
     * WARNING: This code bypasses the sanity check done by misc. low-level
     * services in nuxeo, so you could find yourself setting a state that dopes
     * not exist.
     * 
     * This method makes _a_lot_ of assumption: The session is a LocalSession,
     * data is stored in a SQL database, etc.
     * 
     * In the context of this very specific plug-in, it is ok (notice we don't
     * say "it _probably" is ok"n or "it _should_ be ok" :->.
     */
    protected void customSetCurrentLifecycleState(DocumentModel inDoc,
            String inState) throws DocumentException, LifeCycleException {

        LocalSession localSession = (LocalSession) session;
        Session baseSession = localSession.getSession();

        Document baseDoc = baseSession.getDocumentByUUID(inDoc.getId());
        // SQLDocument sqlDoc = (SQLDocument) baseDoc;
        // sqlDoc.setCurrentLifeCycleState(inState);
        baseDoc.setCurrentLifeCycleState(inState);

    }

}
