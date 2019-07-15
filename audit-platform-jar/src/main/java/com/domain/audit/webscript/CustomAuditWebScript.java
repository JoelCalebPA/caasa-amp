package com.domain.audit.webscript;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.domain.audit.model.Record;

public class CustomAuditWebScript extends DeclarativeWebScript {

    private static Log logger = LogFactory.getLog(CustomAuditWebScript.class);

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
	Map<String, Object> model = new HashMap<String, Object>();
	List<Record> result = new ArrayList<>();

	String regexFec = "^\\d{1,2}?/\\d{1,2}?/\\d{4}?$";
	SimpleDateFormat FORMATO_MDY = new SimpleDateFormat("MM/dd/yyyy");
	SimpleDateFormat FORMATO_YMD_HMS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	try {
	    StringBuffer sb = new StringBuffer();
	    sb.append("http://localhost:8080/alfresco/service/api/audit/query/alfresco-access?verbose=true");

	    String user = req.getParameter("user");
	    String action = req.getParameter("action");
	    String document = req.getParameter("document");
	    String dateIni = req.getParameter("dateIni");
	    String dateFin = req.getParameter("dateFin");

	    if (user != null && user.length() > 0) {
		sb.append("&user=" + user);
	    }
	    if (dateIni != null && Pattern.matches(regexFec, dateIni)) {
		sb.append("&fromTime=" + FORMATO_MDY.parse(dateIni));
	    }
	    if (dateFin != null && Pattern.matches(regexFec, dateFin)) {
		sb.append("&toTime=" + FORMATO_MDY.parse(dateFin));
	    }

	    URL url = new URL(sb.toString());
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("GET");
	    conn.setRequestProperty("Accept", "application/json");

	    Authenticator.setDefault(new Authenticator() {
		protected PasswordAuthentication getPasswordAuthentication() {
		    return new PasswordAuthentication("admin", "admin".toCharArray());
		}
	    });

	    if (conn.getResponseCode() == 200) {
		logger.info("Getting audit trail");
		StringBuffer json = new StringBuffer();
		Scanner sc = new Scanner(conn.getInputStream());
		while (sc.hasNext()) {
		    json.append(sc.nextLine() + "\n");
		}
		sc.close();
		JSONObject obj = new JSONObject(json.toString());
		JSONArray arr = obj.getJSONArray("entries");
		for (int i = 0; i < arr.length(); i++) {
		    String timeT = arr.getJSONObject(i).getString("time");
		    timeT = timeT.replace("T", " ").substring(0, timeT.lastIndexOf(".") - 1);
		    Date dateRs = FORMATO_YMD_HMS.parse(timeT);
		    String userRs = "";
		    String actionRs = "";
		    String documentRs = "";
		    if (!arr.getJSONObject(i).getJSONObject("values").isNull("/alfresco-access/transaction/action")) {
			if (arr.getJSONObject(i).getJSONObject("values").getString("/alfresco-access/transaction/type")
				.equals("cm:content")) {
			    userRs = arr.getJSONObject(i).getJSONObject("values")
				    .getString("/alfresco-access/transaction/user");
			    actionRs = arr.getJSONObject(i).getJSONObject("values")
				    .getString("/alfresco-access/transaction/action");
			    documentRs = arr.getJSONObject(i).getJSONObject("values")
				    .getString("/alfresco-access/transaction/path");
			    Record r = new Record(userRs, dateRs, actionRs,
				    documentRs.substring(documentRs.lastIndexOf(":") + 1));
			    result.add(r);
			}
		    }
		}
	    } else {
		logger.error("Failed : HTTP error code : " + conn.getResponseCode());
	    }
	    if (action != null && action.length() > 0) {
		for (Iterator<Record> iterator = result.iterator(); iterator.hasNext();) {
		    Record record = (Record) iterator.next();
		    String rAct = record.getAction().toUpperCase();
		    if (!rAct.contains(action.toUpperCase())) {
			iterator.remove();
		    }
		}
	    }
	    if (document != null && document.length() > 0) {
		for (Iterator<Record> iterator = result.iterator(); iterator.hasNext();) {
		    Record record = (Record) iterator.next();
		    String rDoc = record.getDocument().toUpperCase();
		    if (!rDoc.contains(document.toUpperCase())) {
			iterator.remove();
		    }
		}
	    }
	    model.put("result", result);
	    conn.disconnect();
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return model;
    }

}
