package ar.gob.gcaba.dgisis.sadetester;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.remote.DesiredCapabilities;

public class DriverFactory {

	public static WebDriver setFirefoxDriver() {
		DesiredCapabilities cp = DesiredCapabilities.firefox();
		cp.setCapability("version", "7");
		cp.setCapability("platform", Platform.LINUX);
		cp.setCapability("selenium-version", "2.18.0");
		
		ProfilesIni allProfiles = new ProfilesIni();
		FirefoxProfile firefox = allProfiles.getProfile("default");
		firefox.setEnableNativeEvents(true);
		
		WebDriver driver = new FirefoxDriver(firefox);
		driver.manage().window().setSize(new Dimension(1280, 800));
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		driver.manage().deleteAllCookies();
		
		return driver;
	}

}
