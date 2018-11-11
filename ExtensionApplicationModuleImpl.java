package sharedApp.extensions;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;

import java.util.Locale;

import javax.naming.NamingException;

import oracle.adf.share.ADFContext;

import oracle.adf.share.security.SecurityContext;

import oracle.jbo.Session;
import oracle.jbo.server.ApplicationModuleImpl;

public class ExtensionApplicationModuleImpl extends ApplicationModuleImpl {
    private HashMap<String, String> map;

    public String getParamValue(String key) {
        return map.get(key);
    }

    public void setParamValue(String key, String value) {
        map.put(key, value);
    }

    public void setLangKaz() {
        setLocale("kk");
    }
    public void setLangRus() {
        setLocale("ru");
    }

    public void setLocale(String locale) {
        String stmt =
            "begin secmgr.sec_ctx.setLangEmp(" + getParamValue("id_person") +
            ",'" + locale + "'); end;";
        java.sql.CallableStatement st = null;
        st = getDBTransaction().createCallableStatement(stmt, 0);
        try {
            st.execute();
            map.put("language", locale);
        } catch (SQLException e) {
            System.out.println("-+- Ошибка установки локали: " + e + " : " +
                               stmt);
        }
        try {
            st.close();
        } catch (SQLException e) {
        }
    }

    public void clearParam() {
        map.clear();
    }

    @Override
    protected void prepareSession(Session session) {
        super.prepareSession(session);
        map = new HashMap<String, String>();

        System.out.println("-+- prepare session started ...");
        getDBTransaction().setClearCacheOnRollback(false);

        //get the authenticated username
        ADFContext adfContext = ADFContext.getCurrent();
        SecurityContext securityCtx = adfContext.getSecurityContext();
        String username = securityCtx.getUserName();
        String ip =
            (String)ADFContext.getCurrent().getSessionScope().get("IP_ADDRESS");
        //define the prepared statement to call the PL/SQL procedure that
        //prepares the application context
        //        System.out.println("-+- set userinfo "+ip);
        String appGetContext =
            "select sys_context('SEC_CTX','id_person') id_person,\n" +
            "    sys_context('SEC_CTX','language') language,\n" +
            "    sys_context('SEC_CTX','lang_territory') lang_territory,\n" +
            "    sys_context('SEC_CTX','roles') roles,\n" +
            "    sys_context('SEC_CTX','id_region') id_region,\n" +
            "    sys_context('SEC_CTX','home_report') home_report,\n" +
            "    sys_context('SEC_CTX','region_name') region_name,\n" +
            "    sys_context('SEC_CTX','region_name_kaz') region_name_kaz\n" +
            "from dual";

        String appSetContext =
            "Begin secmgr.sec_ctx.set_userinfo('" + username + "','" + ip +
            "'); end;";
        //        System.out.println("-+- call: "+appContext);
        ResultSet rs;
        java.sql.CallableStatement st = null;
        String dateFormat =
            //            "ALTER SESSION SET NLS_DATE_FORMAT = 'dd.mm.yyyy'";
            "ALTER SESSION SET NLS_DATE_FORMAT = 'dd.mm.yyyy HH24:MI:SS'";
        try {
            //get ADF BC transaction to execute the statement
            st = getDBTransaction().createCallableStatement(appSetContext, 0);
            st.execute();
            st = getDBTransaction().createCallableStatement(appGetContext, 0);
            rs = st.executeQuery();
            rs.next();
            System.out.println("-+- current row: " + rs.getRow());
            setParamValue("id_person", rs.getString("id_person"));
            setParamValue("language", rs.getString("language"));
            setParamValue("lang_territory", rs.getString("lang_territory"));
            setParamValue("home_report", rs.getString("home_report"));
            setParamValue("roles", rs.getString("roles"));
            System.out.println("-+- Роли: " + getParamValue("roles"));

            setParamValue("id_region", rs.getString("id_region"));

            setParamValue("region_name", rs.getString("region_name"));
            System.out.println("-+- Регион: " + getParamValue("id_region") +
                               ":" + getParamValue("region_name"));

            setParamValue("region_name_kaz", rs.getString("region_name_kaz"));

            rs.close();
            st = getDBTransaction().createCallableStatement(dateFormat, 0);
            st.execute();
            st.close();
        } catch (java.sql.SQLException s) {
            System.out.println("-+- error execute query: " + s);
            throw new oracle.jbo.JboException(s);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
            } catch (java.sql.SQLException s2) {
                System.out.println("-+- prepare session catch error: " + s2);
                //raise exception to handle in ADF
            }
        }

    }

    @Override
    protected void beforeDisconnect() {
        java.sql.CallableStatement st = null;
        String command = "Begin secmgr.sec_ctx.clear_userinfo(); end;";
        try {
            System.out.println("-+- close application");
            st = getDBTransaction().createCallableStatement(command, 0);
            st.execute();
            st.close();
        } catch (SQLException e) {
            System.out.println("-+- error close application: " + e);
        }
        clearParam();
        super.beforeDisconnect();
    }
}
