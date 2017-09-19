package ar.gob.gcaba.dgisis.sadetester;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class LoysTester {
  private WebDriver driver;
  private String baseUrl;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void setUp() throws Exception {
    driver = new FirefoxDriver();
    baseUrl = "https://cas-hml.buenosaires.gob.ar/";
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
  }

  @Test
  public void testLoysTester() throws Exception {
    driver.get(baseUrl + "/");
    driver.findElement(By.id("username")).clear();
    driver.findElement(By.id("username")).sendKeys("ggomez");
    driver.findElement(By.id("password")).clear();
    driver.findElement(By.id("password")).sendKeys("1234");
    driver.findElement(By.name("submit")).click();
    for (int second = 0;; second++) {
    	if (second >= 60) fail("timeout");
    	try { if ("Escritorio Único".equals(driver.getTitle())) break; } catch (Exception e) {}
    	Thread.sleep(1000);
    }

    driver.findElement(By.xpath("//tr[11]/td[2]/div/table/tbody/tr/td/table/tbody/tr/td/img")).click();
    driver.findElement(By.linkText("Inicio LOyS")).click();
    for (int second = 0;; second++) {
    	if (second >= 60) fail("timeout");
    	try { if (isElementPresent(By.id("body:motivoIniciarExpediente"))) break; } catch (Exception e) {}
    	Thread.sleep(1000);
    }

    driver.findElement(By.id("body:motivoIniciarExpediente")).clear();
    driver.findElement(By.id("body:motivoIniciarExpediente")).sendKeys("Test");
    driver.findElement(By.xpath("//tbody[@id='body:data:tbody_element']/tr[3]/td[2]/a/img")).click();
    String expediente = driver.findElement(By.cssSelector("span.x8p.xa")).getText();
    driver.findElement(By.cssSelector("button.x8x")).click();
    driver.findElement(By.xpath("//div[@id='pbPrincipal']/div/table/tbody/tr/td/table/tbody/tr/td[3]/table/tbody/tr/td[3]/a/span")).click();
    driver.findElement(By.xpath("//span")).click();
  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
    String verificationErrorString = verificationErrors.toString();
    if (!"".equals(verificationErrorString)) {
      fail(verificationErrorString);
    }
  }

  private boolean isElementPresent(By by) {
    try {
      driver.findElement(by);
      return true;
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  private boolean isAlertPresent() {
    try {
      driver.switchTo().alert();
      return true;
    } catch (NoAlertPresentException e) {
      return false;
    }
  }

  private String closeAlertAndGetItsText() {
    try {
      Alert alert = driver.switchTo().alert();
      String alertText = alert.getText();
      if (acceptNextAlert) {
        alert.accept();
      } else {
        alert.dismiss();
      }
      return alertText;
    } finally {
      acceptNextAlert = true;
    }
  }
}
