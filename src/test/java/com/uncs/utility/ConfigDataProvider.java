package com.uncs.utility;

import com.uncs.support.MailCredentials;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class ConfigDataProvider {

    Properties pro;

    public ConfigDataProvider() {

        File src = new File("./Config/config.properties");

        try {
            FileInputStream fis = new FileInputStream(src);

            pro = new Properties();

            pro.load(fis);

        } catch (Exception e) {
            System.out.println("Not able to load Config File" + e.getMessage());

        }
    }

    public String getUrl() {
        return pro.getProperty("url");
    }

    public String getEmail() {
        return pro.getProperty("email");
    }

    /**
     * Whether Chrome should run without a visible window. Defaults to false, so a
     * missing property keeps the old behaviour of showing the browser.
     *
     * {@link BrowserFactory#isHeadless()} lets -Dheadless=true win over this, so a
     * one-off headless run needs no edit here.
     */
    public boolean isHeadless() {

        String headless = pro.getProperty("headless");

        if (headless == null || headless.isBlank()) {
            return false;
        }
        return Boolean.parseBoolean(headless.trim());
    }

    /**
     * The account password from the environment.
     *
     * Only a fallback now: {@link #resolvePassword(String)} prefers what the test data
     * workbook holds, because an environment variable set in one terminal does not reach
     * an IDE or a shell that is already running, which made local runs fail while
     * automated ones passed.
     * PowerShell:  $env:UNCS_PASSWORD = 'yourPassword'
     */
    public String getPassword() {
        return System.getenv("UNCS_PASSWORD");
    }

    public String getMailHost() {
        return pro.getProperty("mail.smtp.host");
    }

    public String getMailPort() {
        return pro.getProperty("mail.smtp.port");
    }

    public String getMailFrom() {
        return pro.getProperty("mail.from");
    }

    public String getMailTo() {
        return pro.getProperty("mail.to");
    }

    public String getMailAuth() {
        return pro.getProperty("mail.smtp.auth");
    }

    public String getMailStartTLS() {
        return pro.getProperty("mail.smtp.starttls.enable");
    }

    /**
     * Never read from config.properties, so the mailbox app password stays out of source
     * control. {@link MailCredentials} decides where it does come from.
     *
     * An empty result is not an error: the report email is skipped and the run stands on
     * its own result.
     */
    public String getMailPassword() {

        String password = MailCredentials.password();

        if (password.isEmpty()) {
            System.out.println("No mail password found in " + MailCredentials.source()
                    + ". The report email will be skipped.");
            return null;
        }

        return password;
    }

    /**
     * Picks the password for a run. The test data workbook is checked in, so the
     * shipped Login sheet carries the word ENV rather than a real password and the
     * secret keeps coming from the environment. Put a literal password in the cell
     * only for a throwaway account.
     */
    public String resolvePassword(String fromTestData) {

        if (fromTestData != null && !fromTestData.isBlank() && !fromTestData.equalsIgnoreCase("ENV")) {
            return fromTestData;
        }

        return getPassword();
    }
}
