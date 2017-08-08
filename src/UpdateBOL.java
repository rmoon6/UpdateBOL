import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chase.Moon on 8/3/2017.
 *
 * This is a script that will be used to generate sql to update BOLs to specified values for specified containers
 *
 * This will be accomplished by doing the following:
 *
 * 1.   Reading container and BOL values into a data structures
 * 2.   Looping through these data structures and writing the SQLs
 */
public class UpdateBOL {

    private static final String IN_PATH = "C:\\Users\\Chase.Moon\\IdeaProjects\\UpdateBOL\\res\\bolUpdate.asc";
    private static final String OUT_PATH = "C:\\Users\\Chase.Moon\\IdeaProjects\\UpdateBOL\\res\\bolUpdate.sql";

    private static final String BACKUP_DUMP_PATH = "C:\\Dump\\BOLUpdate\\";

    private PrintWriter pwBol;
    private List<UpdateValue> updateValueList;

    public static void main(String[] args) {

        UpdateBOL updateBOL = new UpdateBOL();
        updateBOL.initialize();
        updateBOL.readData();
        updateBOL.printInitialComments();
        updateBOL.writeBackupSQL(false);
        updateBOL.writeUpdateSQL();
        updateBOL.writeBackupSQL(true);
        updateBOL.closeDown();

    }

    private void closeDown() {

        pwBol.close();

    }

    private void printInitialComments() {
        pwBol.println("/*");
        pwBol.println("\tLIST OF CONTAINERS AND UPDATED BOLS [container\t:\tBOL]");

        for (UpdateValue uv : updateValueList) {
            pwBol.println("\t" + uv.getContainer() + "\t:\t" + uv.getBol());
        }

        pwBol.println("*/");
    }

    private void writeBackupSQL(boolean isAfter) {
        printManifestBackupSQL(isAfter);
        printCtcBackupSQL(isAfter);
        printTbsBackupSQL(isAfter);
        printSiBackupSQL(isAfter);
    }

    private void printManifestBackupSQL(boolean isAfter) {
        pwBol.println();

        pwBol.println("/*MANIFEST BACKUP*/");
        pwBol.println("select");
        pwBol.println("\tctl.customer_id,");
        pwBol.println("\tctl.container_id,");
        pwBol.println("\tman.manifest_id,");
        pwBol.println("\tman.trans_item_bl_awb_");
        pwBol.println("from");
        pwBol.println("\tcntr_trans_leg ctl");
        pwBol.println("\tkey join cntr_trans_leg_activity ctla,");
        pwBol.println("\tmanifest man");
        pwBol.println("where ctla.base_leg = 1");
        pwBol.println("and man.customer_id = CTL.customer_id");
        pwBol.println("and man.container_id = CTL.container_id");
        pwBol.println("and man.cntr_office_code = CTL.cntr_office_code");
        pwBol.println("and man.container_trip = CTL.container_trip");
        pwBol.println("and (");

        String currentContainer = "";
        for (int i = 0; i < updateValueList.size(); i++) {

            currentContainer = updateValueList.get(i).getContainer();
            if (currentContainer.length() == 11) {
                currentContainer = currentContainer.substring(0, currentContainer.length()-1);
            } else if (!(currentContainer.length() == 11 ||
                    currentContainer.length() == 10)) {
                continue;
            }

            if (i == 0) {
                pwBol.println("\tctl.container_id like '" + currentContainer + "%'");
            } else {
                pwBol.println("\tor ctl.container_id like '" + currentContainer + "%'");
            }
        }

        pwBol.println(");");

        if (isAfter) {
            pwBol.println("output to '" + BACKUP_DUMP_PATH + "output_MAN_after.asc' " +
                    "delimited by ',' quote '' format ascii;");
        } else {
            pwBol.println("output to '" + BACKUP_DUMP_PATH + "output_MAN.asc' " +
                    "delimited by ',' quote '' format ascii;");
        }

        pwBol.println();
    }

    private void printCtcBackupSQL(boolean isAfter) {
        pwBol.println("/*CNTR_TRNS_COSTS UPDATE*/");
        pwBol.println("select");
        pwBol.println("\tctl.customer_id,");
        pwBol.println("\tctl.container_id,");
        pwBol.println("\tctc.container_trans_in");
        pwBol.println("from");
        pwBol.println("\tcntr_trans_leg ctl key join");
        pwBol.println("\tcntr_trns_costs ctc key join");
        pwBol.println("\tcntr_trans_leg_activity ctla");
        pwBol.println("where ctla.base_leg = 1");
        pwBol.println("and (");

        String currentContainer = "";
        for (int i = 0; i < updateValueList.size(); i++) {

            currentContainer = updateValueList.get(i).getContainer();
            if (currentContainer.length() == 11) {
                currentContainer = currentContainer.substring(0, currentContainer.length()-1);
            } else if (!(currentContainer.length() == 11 ||
                    currentContainer.length() == 10)) {
                continue;
            }

            if (i == 0) {
                pwBol.println("\tctl.container_id like '" + currentContainer + "%'");
            } else {
                pwBol.println("\tor ctl.container_id like '" + currentContainer + "%'");
            }
        }

        pwBol.println(");");

        if (isAfter) {
            pwBol.println("output to '" + BACKUP_DUMP_PATH + "output_CTC_after.asc' " +
                    "delimited by ',' quote '' format ascii;");
        } else {
            pwBol.println("output to '" + BACKUP_DUMP_PATH + "output_CTC.asc' " +
                    "delimited by ',' quote '' format ascii;");
        }

        pwBol.println();

    }

    private void printTbsBackupSQL(boolean isAfter) {
        pwBol.println("/*THD_BKG_STATUSES BACKUP*/");
        pwBol.println("select");
        pwBol.println("\tctl.customer_id,");
        pwBol.println("\tctl.container_id,");
        pwBol.println("\ttbs.booking_num,");
        pwBol.println("\ttbs.bl_with_scac");
        pwBol.println("from");
        pwBol.println("\tcntr_trans_leg ctl key join");
        pwBol.println("\tcntr_trans_leg_activity ctla,");
        pwBol.println("\tmanifest man key join");
        pwBol.println("\tshipment_item si,");
        pwBol.println("\tthd_bkg_statuses tbs");
        pwBol.println("where ctla.base_leg = 1");
        pwBol.println("and man.customer_id = CTL.customer_id");
        pwBol.println("and man.container_id = CTL.container_id");
        pwBol.println("and man.cntr_office_code = CTL.cntr_office_code");
        pwBol.println("and man.container_trip = CTL.container_trip");
        pwBol.println("and tbs.customer_id = si.customer_id");
        pwBol.println("and tbs.booking_num = si.booking_num");
        pwBol.println("and tbs.order_key = si.order_key");
        pwBol.println("and tbs.bk_itm = si.bk_itm");
        pwBol.println("and tbs.itm_orig = SI.itm_orig");
        pwBol.println("and tbs.item_dest = si.item_dest");
        pwBol.println("and (");

        String currentContainer = "";
        for (int i = 0; i < updateValueList.size(); i++) {

            currentContainer = updateValueList.get(i).getContainer();
            if (currentContainer.length() == 11) {
                currentContainer = currentContainer.substring(0, currentContainer.length()-1);
            } else if (!(currentContainer.length() == 11 ||
                    currentContainer.length() == 10)) {
                continue;
            }

            if (i == 0) {
                pwBol.println("\tctl.container_id like '" + currentContainer + "%'");
            } else {
                pwBol.println("\tor ctl.container_id like '" + currentContainer + "%'");
            }
        }

        pwBol.println(");");

        if (isAfter) {
            pwBol.println("output to '" + BACKUP_DUMP_PATH + "output_TBS_after.asc' " +
                    "delimited by ',' quote '' format ascii;");
        } else {
            pwBol.println("output to '" + BACKUP_DUMP_PATH + "output_TBS.asc' " +
                    "delimited by ',' quote '' format ascii;");
        }

        pwBol.println();
    }

    private void printSiBackupSQL(boolean isAfter) {
        pwBol.println("/*SHIPMENT_ITEM BACKUP*/");
        pwBol.println("select");
        pwBol.println("\tctl.customer_id,");
        pwBol.println("\tctl.container_id,");
        pwBol.println("\tsi.shipment_reference,");
        pwBol.println("\tsi.shp_itm_bl");
        pwBol.println("from");
        pwBol.println("\tcntr_trans_leg ctl key join");
        pwBol.println("\tcntr_trans_leg_activity ctla,");
        pwBol.println("\tmanifest man key join");
        pwBol.println("\tshipment_item si");
        pwBol.println("where ctla.base_leg = 1");
        pwBol.println("and man.customer_id = CTL.customer_id");
        pwBol.println("and man.container_id = CTL.container_id");
        pwBol.println("and man.cntr_office_code = CTL.cntr_office_code");
        pwBol.println("and man.container_trip = CTL.container_trip");
        pwBol.println("and (");

        String currentContainer = "";
        for (int i = 0; i < updateValueList.size(); i++) {

            currentContainer = updateValueList.get(i).getContainer();
            if (currentContainer.length() == 11) {
                currentContainer = currentContainer.substring(0, currentContainer.length()-1);
            } else if (!(currentContainer.length() == 11 ||
                    currentContainer.length() == 10)) {
                continue;
            }

            if (i == 0) {
                pwBol.println("\tctl.container_id like '" + currentContainer + "%'");
            } else {
                pwBol.println("\tor ctl.container_id like '" + currentContainer + "%'");
            }
        }

        pwBol.println(");");

        if (isAfter) {
            pwBol.println("output to '" + BACKUP_DUMP_PATH + "output_SI_after.asc' " +
                    "delimited by ',' quote '' format ascii;");
        } else {
            pwBol.println("output to '" + BACKUP_DUMP_PATH + "output_SI.asc' " +
                    "delimited by ',' quote '' format ascii;");
        }

        pwBol.println();
    }

    private void writeUpdateSQL() {
        String currentContainer;
        String currentBOL;

        for (UpdateValue uv : updateValueList) {
            currentContainer = uv.getContainer();
            currentBOL = uv.getBol();

            if (currentContainer.length() == 11) {
                currentContainer = currentContainer.substring(0, currentContainer.length()-1);
            } else if (!(currentContainer.length() == 11 ||
                            currentContainer.length() == 10)) {
                continue;
            }

            pwBol.println();
            pwBol.println("/*");
            pwBol.println("\tFOR CONTAINER " + uv.getContainer() + " UPDATE BOL TO " + uv.getBol());
            pwBol.println("*/");

            printManifestUpdateSQL(currentContainer, currentBOL);
            pwBol.println();
            printCtcUpdateSql(currentContainer, currentBOL);
            pwBol.println();
            printTbsUpdateSql(currentContainer, currentBOL);
            pwBol.println();
            printSiUpdateSql(currentContainer, currentBOL);

        }

        pwBol.println();
        pwBol.println("commit;");

    }

    private void printManifestUpdateSQL(String container, String bol) {
        pwBol.println("/*MANIFEST UPDATE*/");
        pwBol.println("update manifest");
        pwBol.println("set trans_item_bl_awb_ = '" + bol + "'");
        pwBol.println("from");
        pwBol.println("\tcntr_trans_leg ctl key join");
        pwBol.println("\tcntr_trans_leg_activity ctla,");
        pwBol.println("\tmanifest man");
        pwBol.println("where man.customer_id = CTL.customer_id");
        pwBol.println("and man.container_id = CTL.container_id");
        pwBol.println("and man.cntr_office_code = CTL.cntr_office_code");
        pwBol.println("and man.container_trip = CTL.container_trip");
        pwBol.println("and ctla.base_leg = 1");
        pwBol.println("and ctl.container_id like '" + container + "%';");
    }

    private void printCtcUpdateSql(String container, String bol) {
        pwBol.println("/*CNTR_TRNS_COSTS UPDATE*/");
        pwBol.println("update cntr_trns_costs");
        pwBol.println("set container_trans_in = '" + bol + "'");
        pwBol.println("from");
        pwBol.println("\tcntr_trans_leg ctl key join");
        pwBol.println("\tcntr_trns_costs ctc key join");
        pwBol.println("\tcntr_trans_leg_activity ctla");
        pwBol.println("where ctla.base_leg = 1");
        pwBol.println("and ctl.container_id like '" + container + "%';");

    }

    private void printTbsUpdateSql(String container, String bol) {
        pwBol.println("/*THD_BKG_STATUSES UPDATE*/");
        pwBol.println("update thd_bkg_statuses");
        pwBol.println("set bl_with_scac = '" + bol + "'");
        pwBol.println("from");
        pwBol.println("\tcntr_trans_leg ctl key join");
        pwBol.println("\tcntr_trans_leg_activity ctla,");
        pwBol.println("\tmanifest man key join");
        pwBol.println("\tshipment_item si,");
        pwBol.println("\tthd_bkg_statuses tbs");
        pwBol.println("where ctla.base_leg = 1");
        pwBol.println("and man.customer_id = CTL.customer_id");
        pwBol.println("and man.container_id = CTL.container_id");
        pwBol.println("and man.cntr_office_code = CTL.cntr_office_code");
        pwBol.println("and man.container_trip = CTL.container_trip");
        pwBol.println("and tbs.customer_id = si.customer_id");
        pwBol.println("and tbs.booking_num = si.booking_num");
        pwBol.println("and tbs.order_key = si.order_key");
        pwBol.println("and tbs.bk_itm = si.bk_itm");
        pwBol.println("and tbs.itm_orig = SI.itm_orig");
        pwBol.println("and tbs.item_dest = si.item_dest");
        pwBol.println("and ctl.container_id like '" + container + "%';");
    }

    private void printSiUpdateSql(String container, String bol) {
        pwBol.println("/*SHIPMENT_ITEM UPDATE*/");
        pwBol.println("update shipment_item");
        pwBol.println("set shp_itm_bl = '" + bol + "'");
        pwBol.println("from");
        pwBol.println("\tcntr_trans_leg ctl key join");
        pwBol.println("\tcntr_trans_leg_activity ctla,");
        pwBol.println("\tmanifest man key join");
        pwBol.println("\tshipment_item si");
        pwBol.println("where ctla.base_leg = 1");
        pwBol.println("and man.customer_id = CTL.customer_id");
        pwBol.println("and man.container_id = CTL.container_id");
        pwBol.println("and man.cntr_office_code = CTL.cntr_office_code");
        pwBol.println("and man.container_trip = CTL.container_trip");
        pwBol.println("and ctl.container_id like '" + container + "%';");
    }

    private void initialize() {

        try {
            pwBol = new PrintWriter(OUT_PATH);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        updateValueList = new ArrayList<>();

    }

    private void readData() {
        String line = "";
        String[] lineSplit;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(IN_PATH));

            while ((line = br.readLine()) != null) {
                lineSplit = line.split(",");
                updateValueList.add(new UpdateValue(lineSplit[0], lineSplit[1]));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void printUpdateList() {
        for (UpdateValue uv : updateValueList) {
            System.out.println(uv);
        }
    }

    private class UpdateValue {

        private String container;
        private String bol;

        public UpdateValue(String container, String bol) {
            this.container = container;
            this.bol = bol;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Container: ");
            sb.append(container + "\t");
            sb.append("BOL: ");
            sb.append(bol);

            return sb.toString();
        }

        public void setContainer(String container) {
            this.container = container;
        }

        public void setBol(String bol) {
            this.bol = bol;
        }

        public String getContainer() {
            return container;
        }

        public String getBol() {
            return bol;
        }

    }

}







































