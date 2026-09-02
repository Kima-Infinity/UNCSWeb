package com.uncs.utility;

import com.uncs.support.MailCredentials;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Sends the run's report to whoever is configured to receive it.
 *
 * Nothing here can fail a run. A mailbox that is misconfigured, unreachable or simply not
 * set up yet is reported to the console and otherwise ignored - the tests have already
 * finished by the time this is called, and losing their result to a mail problem would be
 * worse than not sending it.
 */
public class MailUtils {

	public static void sendEmail(String host, String port, String auth, String starttls,
	                             final String from, final String password, String to,
	                             String subject, String body, List<String> attachmentPaths) {

		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		props.put("mail.smtp.auth", auth);
		props.put("mail.smtp.starttls.enable", starttls);

		if ("465".equals(port)) {
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.ssl.enable", "true");
			props.put("mail.smtp.starttls.enable", "false");
		}

		props.put("mail.smtp.ssl.protocols", "TLSv1.2");
		props.put("mail.smtp.ssl.trust", "*");
		props.put("mail.smtp.connectiontimeout", "10000");
		props.put("mail.smtp.timeout", "10000");

		/*
		 * Try every configured credential, not only the one handed in.
		 *
		 * The most explicit source is not always the working one: a revoked app password
		 * left behind in an environment variable shadows a good one in the secrets file,
		 * and the send then fails as 535-5.7.8 while a usable credential sits unused on
		 * the same machine. Actually sending is what settles which credential is real.
		 */
		Map<String, String> attempts = new LinkedHashMap<>();

		if (password != null && !password.isBlank()) {
			attempts.put("the password supplied by the caller", password);
		}

		for (Map.Entry<String, String> candidate : MailCredentials.candidates().entrySet()) {
			if (!attempts.containsValue(candidate.getValue())) {
				attempts.put(candidate.getKey(), candidate.getValue());
			}
		}

		if (attempts.isEmpty()) {
			System.out.println("No mail password is configured (" + MailCredentials.source()
					+ "). Skipping the report email.");
			return;
		}

		for (Map.Entry<String, String> attempt : attempts.entrySet()) {

			if (send(props, from, attempt.getValue(), to, subject, body, attachmentPaths)) {
				System.out.println("Report email sent using " + attempt.getKey());
				return;
			}

			System.out.println("Report email was refused with " + attempt.getKey());
		}

		System.out.println("No configured mail credential could send the report email.");
	}

	/** One send attempt with one credential. Returns whether it went out. */
	private static boolean send(Properties props, final String from, final String password,
	                            String to, String subject, String body,
	                            List<String> attachmentPaths) {

		Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(from, password);
			}
		});

		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject(subject);

			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(body, "text/html; charset=utf-8");

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			if (attachmentPaths != null) {

				for (String attachmentPath : attachmentPaths) {

					if (attachmentPath == null || attachmentPath.isBlank()) {
						continue;
					}

					File attachmentFile = new File(attachmentPath);

					if (!attachmentFile.exists()) {
						System.out.println("Skipping missing attachment: " + attachmentPath);
						continue;
					}

					MimeBodyPart attachmentPart = new MimeBodyPart();
					DataSource source = new FileDataSource(attachmentPath);
					attachmentPart.setDataHandler(new DataHandler(source));
					attachmentPart.setFileName(attachmentFile.getName());

					// The screenshot is shown in the body rather than hung off the bottom,
					// so the last thing the run saw is visible without opening anything.
					if (attachmentPath.toLowerCase().endsWith(".png")) {
						attachmentPart.setHeader("Content-ID", "<screenshot>");
						attachmentPart.setDisposition(MimeBodyPart.INLINE);
					} else {
						attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT);
					}

					multipart.addBodyPart(attachmentPart);
				}
			}

			message.setContent(multipart);
			Transport.send(message);

			return true;

		} catch (Exception e) {
			// Not a stack trace: a refused credential is an expected outcome here, and the
			// caller simply moves on to the next one.
			System.out.println("Could not send the report email: " + e.getMessage());
			return false;
		}
	}
}
