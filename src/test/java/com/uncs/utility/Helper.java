package com.uncs.utility;

import com.microsoft.playwright.Page;

import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Helper {

	public static String captureScreenShot(Page page) {

		String screenShotPath = System.getProperty("user.dir") + "/Screenshots/" + getCurrentDateTime() + "screenshot.png";

		try {
			// Playwright writes straight to the path it is given, so the copy from a temp
			// file that Selenium needed is gone. Full page rather than just the viewport:
			// a failure is often something below the fold, and this costs nothing.
			page.screenshot(new Page.ScreenshotOptions()
					.setPath(Paths.get(screenShotPath))
					.setFullPage(true));
		} catch (Exception e) {
			System.out.println("Not able to capture screenshot" + e.getMessage());
		}

		return screenShotPath;
	}

	public static String getCurrentDateTime() {

		DateFormat customFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
		Date date = new Date();
		return customFormat.format(date);

	}

	/**
	 * A message that is easy to pick out of a real conversation later, and different on
	 * every run so the assertion cannot pass on a bubble an earlier run left behind.
	 */
	public static String getUniqueMessage(String baseMessage) {

		return baseMessage + " " + new SimpleDateFormat("ddMMyyyy HH:mm:ss").format(new Date());

	}
}
