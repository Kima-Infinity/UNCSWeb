package com.uncs.testcases;

import com.uncs.pages.LoginPage;
import com.uncs.utility.BrowserFactory;
import com.uncs.utility.ConfigDataProvider;
import com.uncs.utility.ExcelDataProvider;
import com.microsoft.playwright.Page;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * The standalone TestNG sign in, kept alongside the Cucumber suite as the quickest way to
 * check that the environment is reachable and the credentials still work.
 *
 * Both the address and the password come from Login_TestData.xlsx, the same sheet and row
 * the Cucumber login step reads, so there is one place to change when the account changes
 * rather than two that can drift apart.
 */
public class LoginTest {

	private static final String DATA_FILE = "Login_TestData.xlsx";
	private static final String SHEET = "Sheet1";

	/** Row 0 holds the headers, so the first row of real data is row 1. */
	private static final int ROW = 1;

	private static final int EMAIL_COLUMN = 1;
	private static final int PASSWORD_COLUMN = 2;

	Page driver;

	ConfigDataProvider config;

	ExcelDataProvider excel;

	@BeforeClass
	public void setUp() {

		config = new ConfigDataProvider();

		excel = new ExcelDataProvider(DATA_FILE, SHEET);

		driver = BrowserFactory.startBrowser(config.getUrl());

	}

	@Test
	public void loginWithEmail() {

		String email = excel.getStringData(SHEET, ROW, EMAIL_COLUMN);

		/*
		 * resolvePassword takes what the sheet holds, and falls back to the UNCS_PASSWORD
		 * environment variable only when the cell is empty or still says ENV. That keeps
		 * this working whichever way the account is configured.
		 */
		String password = config.resolvePassword(excel.getStringData(SHEET, ROW, PASSWORD_COLUMN));

		assertNotNull(password, "No password available. Put one in the Password column of "
				+ DATA_FILE + " row " + ROW + ", or set the UNCS_PASSWORD environment variable.");

		LoginPage loginPage = new LoginPage(driver);

		loginPage.loginToUNCS(email, password);

		assertTrue(loginPage.isLoggedIn(), "Still on the login page - login did not succeed");

		System.out.println("Test Completed!");

	}

	@AfterClass(alwaysRun = true)
	public void tearDown() {

		if (driver != null) {
			BrowserFactory.quitBrowser(driver);
		}

	}

}
