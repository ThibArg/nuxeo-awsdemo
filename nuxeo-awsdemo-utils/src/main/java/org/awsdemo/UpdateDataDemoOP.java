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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WARNING: QUick dev, put in this plug-in for convenience because we laready
 * have the utilities
 * 
 */
@Operation(id = UpdateDataDemoOP.ID, category = Constants.CAT_SERVICES, label = "UpdateDataDemoOP", description = "")
public class UpdateDataDemoOP {

    public static final String ID = "UpdateDataDemoOP";

    private static final Log log = LogFactory.getLog(UpdateDataDemoOP.class);

    protected static final int COMMIT_MODULO = 50;

    protected static final String[] MODIF_USERS = { "john", "john", "john",
            "jim", "kate", "kate", "kate", "kate", "external1", "external2",
            "user1", "user2" };

    protected static final int MODIF_USERS_MAX = MODIF_USERS.length - 1;

    protected static final String[] COUNTRIES = { "US", "US", "US", "US", "FR",
            "GB", "CA", "JP", "JP" };

    protected static final int COUNTRIES_MAX = COUNTRIES.length - 1;

    protected static final String[] SUBJECTS = { "Outdoor", "Outdoor",
            "Outdoor", "Outdoor", "Sport", "Sport", "Fashion", "Winter",
            "Winter", "Winter", "Summer", "Indoor" };

    protected static final int SUBJECTS_MAX = SUBJECTS.length - 1;

    protected long todayAsMS;

    protected int count = 0;

    protected String parentPath;

    protected DateFormat _yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected Calendar _today = Calendar.getInstance();

    @Context
    protected CoreSession session;

    @Param(name = "updateLifecycleState", required = false)
    protected boolean updateLifecycleState;

    @OperationMethod
    public void run() {

        log.warn("Updating ...");

        todayAsMS = Calendar.getInstance().getTimeInMillis();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        String nxql = "SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'deleted' AND ecm:primaryType IN ('Video', 'Picture', 'File')";
        DocumentModelList docs = session.query(nxql);

        count = 0;
        for (DocumentModel doc : docs) {

            doUpdateDoc(doc);

            count += 1;
            if ((count % 50) == 0) {
                log.warn("Updated: " + count);
            }
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("...updating done.");

    }

    protected void doUpdateDoc(DocumentModel doc) {

        doc.setPropertyValue("dc:creator",
                MODIF_USERS[RandomValues.randomInt(0, MODIF_USERS_MAX)]);
        doc.setPropertyValue("dc:lastContributor",
                MODIF_USERS[RandomValues.randomInt(0, MODIF_USERS_MAX)]);

        Calendar created = RandomValues.buildDate(null, 20, 360, true);
        doc.setPropertyValue("dc:created", created);

        Calendar modified = RandomValues.addDays(created, 20, true);
        doc.setPropertyValue("dc:modified", modified);

        doc.setPropertyValue("dc:coverage",
                COUNTRIES[RandomValues.randomInt(0, COUNTRIES_MAX)]);

        int countSubjects = RandomValues.randomInt(1, 3);
        List<String> subjects = new ArrayList<String>();
        for (int i = 0; i < countSubjects; i++) {
            String str;
            do {
                str = SUBJECTS[RandomValues.randomInt(0, SUBJECTS_MAX)];
            } while (subjects.contains(str));
            subjects.add(str);
        }
        String[] finalSubjects = new String[countSubjects];
        subjects.toArray(finalSubjects);
        doc.setPropertyValue("dc:subjects", finalSubjects);

        saveTheDoc(doc);

        if (updateLifecycleState) {
            updateTheLifecycleState(doc);
        }

    }

    protected void updateTheLifecycleState(DocumentModel inDoc) {

        String lfs = inDoc.getCurrentLifeCycleState();

        if (lfs.equals("project")) {
            int r = RandomValues.randomInt(1, 10);
            if (r > 3) {
                inDoc.followTransition("approve");
            } else if (r > 1) {
                inDoc.followTransition("obsolete");
            }
        }
    }

    protected void saveTheDoc(DocumentModel inDoc) {

        // Disable dublincore
        inDoc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                true);
        // Make sure our custom events are not triggered
        // (see Studio project) +> we don't want to start a workflow for example
        inDoc.putContextData("UpdatingData_NoEventPlease", true);
        // MARCHE PÔ...

        session.saveDocument(inDoc);

        if ((count % COMMIT_MODULO) == 0) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

    }

}
