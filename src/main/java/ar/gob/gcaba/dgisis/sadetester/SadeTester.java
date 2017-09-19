package ar.gob.gcaba.dgisis.sadetester;

import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.By.ByXPath;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class SadeTester {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SadeTester.class);
	private static final String EE_TITLE = "Sistema Expediente Electrónico";

	private static Options options = new Options();
	private String ambiente = "dev";
	private String username;
	private String password;
	private static boolean debug = false;
	private ModuloSADE modulo = ModuloSADE.FULL;
	private String baseURL;
	private boolean acceptNextAlert = true;
	private StopWatch sw;
	private static final String TEST_TEXT = "Test performance. Carece de motivación administrativa.";

	private static String tipo = "oper";
	private WebDriver driver;

	public static void main(String[] args) {
		LOGGER.info("============================================================");
		new SadeTester(args).test(DriverFactory.setFirefoxDriver());
	}

	public void test(final WebDriver theDriver) {
		setDriver(theDriver);
		try {
			setup();
			login();
			run();
			logout();
		} catch (SadeTesterException e) {
			LOGGER.info(
					"No puede conectarse con el ambiente '{}' de SADE. Por favor, verifique manualmente.",
					ambiente);
			LOGGER.debug("Error: {}", e.getLocalizedMessage());
			System.exit(1);
		} finally {
			close();
		}

	}

	void run() {
		switch (modulo) {
		case TRACK:
			testTRACK();
			break;

		case CCOO:
			testCCOO();
			break;

		case PF:
			testPF();
			break;

		case GEDO:
			testGEDO();
			break;

		case EE:
			testEE();
			break;

		case LOYS:
			testLOYS();
			break;

		default:
			testTRACK();
			testCCOO();
			testPF();
			testGEDO();
			testEE();
			testLOYS();
			break;
		}

	}

	void setup() throws SadeTesterException {
		// Normalmente debería venir inyectado (real o mock) pero si no, uso el
		// default Firefox.
		if (driver == null) {
			LOGGER.debug("Driver null, inyectando Firefox por default...");
			driver = new FirefoxDriver();
			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		}

		baseURL = "http://eu." + ambiente + ".gcba.gob.ar";

		for (int i = 0;; i++) {
			if (i > 2)
				throw new SadeTesterException(
						"No se pudo cargar la página inicial en 3 intentos.");

			sw = new Slf4JStopWatch(LOGGER, Slf4JStopWatch.DEBUG_LEVEL);

			// open | / |
			driver.get(baseURL + "/");
			if (assertTitle("Sistema de Administración de Documentos Electrónicos")) {
				LOGGER.info("Entrando a SADE");
				LOGGER.info("Carga página {}: {} segundos.", baseURL,
						sw.getElapsedTime() / 1000.0);
				sw.lap("Carga Inicial", baseURL);
				break;
			} else {
				LOGGER.info("No carga la página {}, intento #{}...", baseURL,
						i + 1);
			}
		}
	}

	void login() throws SadeTesterException {
		// type | id=username | [usuario]
		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys(username);
		// type | id=password | *****
		driver.findElement(By.id("password")).clear();
		driver.findElement(By.id("password")).sendKeys(password);
		// click | name=submit |
		driver.findElement(By.name("submit")).click();
		// waitForTitle | Escritorio Único |
		for (int second = 0;; second++) {
			if (second >= 10) {
				LOGGER.info("No se pudo determinar el login exitoso en el tiempo establecido.");
				throw new SadeTesterException(
						"No se pudo determinar el login exitoso en el tiempo establecido.");
			}
			try {
				if (assertTitle("Escritorio Único")) {
					sw.stop("Login", "Ambiente: " + ambiente);
					LOGGER.info("Tiempo de Login: {} segundos.",
							sw.getElapsedTime() / 1000.0);
					break;
				} else {
					LOGGER.debug("Página incorrecta ({}), reintenta.",
							driver.getTitle());
				}
			} catch (Exception e) {
				LOGGER.debug("Excepción al esperar Escritorio Unico.", e);
			}
			sleep(1000);
		}
	}

	void logout() {

		if (!assertTitle("Escritorio Único")) {

			LOGGER.debug("Volviendo a escritorio.");
			driver.get(baseURL + "/eu-web");
		}
		LOGGER.debug("Salir");
		driver.findElement(ByXPath.xpath("//span[text()=\"Salir\"]")).click(); // Salir
		sleep(1000);
		if (assertTitle("Sistema de Administración de Documentos Electrónicos")) {
			LOGGER.debug("Logout exitoso.");
			driver.close();
		} else {
			LOGGER.debug(
					"No volvió a la pantalla de login, abortando desde {}.",
					driver.getTitle());
		}
	}

	void testTRACK() {
		int reintentos = 0;
		while (true) {
			driver.get(baseURL + "/eu-web");
			if (reintentos > 2) {
				LOGGER.info("No se pudo comprobar si TRACK está operativo.");
				return;
			}
			try {
				testTRACKOperativo();
				break;
			} catch (Exception e) {
				LOGGER.debug("Falla TRACKOperativo ({}), reintento #{}",
						e.getLocalizedMessage(), ++reintentos);
			}

		}
		if ("perf".equalsIgnoreCase(tipo)) {
			reintentos = 0;
			while (true) {
				if (reintentos > 2) {
					LOGGER.info("No se pudo comprobar la performance de TRACK");
					return;
				}
				try {
					testTRACKPerformance();
					break;
				} catch (Exception e) {
					LOGGER.debug("Falla TRACKPerformance ({}), reintento #{}",
							e.getLocalizedMessage(), ++reintentos);
				}

			}
		}
	}

	void testCCOO() {
		int reintentos = 0;
		while (true) {
			driver.get(baseURL + "/eu-web");
			if (reintentos > 2) {
				LOGGER.info("No se pudo comprobar si CCOO está operativo.");
				return;
			}
			try {
				testCCOOOperativo();
				break;
			} catch (Exception e) {
				LOGGER.debug("Falla CCOOOperativo ({}), reintento #{}",
						e.getLocalizedMessage(), ++reintentos);
			}

		}
		if ("perf".equalsIgnoreCase(tipo)) {
			reintentos = 0;
			while (true) {
				if (reintentos > 2) {
					LOGGER.info("No se pudo comprobar la performance de CCOO");
					return;
				}
				try {
					testCCOOPerformance();
					break;
				} catch (Exception e) {
					LOGGER.debug("Falla CCOOPerformance ({}), reintento #{}",
							e.getLocalizedMessage(), ++reintentos);
					try {
						testCCOOOperativo();
					} catch (SadeTesterException e1) {
						LOGGER.debug("Error al reintentar CCOO: {}",
								e1.getLocalizedMessage());
					}
				}

			}
		}
	}

	void testPF() {
		int reintentos = 0;
		while (true) {
			driver.get(baseURL + "/eu-web");
			if (reintentos > 2) {
				LOGGER.info("No se pudo comprobar si PF está operativo.");
				return;
			}
			try {
				testPFOperativo();
				break;
			} catch (Exception e) {
				LOGGER.debug("Falla PFOperativo ({}), reintento #{}",
						e.getLocalizedMessage(), ++reintentos);
			}

		}
		if ("perf".equalsIgnoreCase(tipo)) {
			reintentos = 0;
			while (true) {
				if (reintentos > 2) {
					LOGGER.info("No se pudo comprobar la performance de PF");
					return;
				}
				try {
					testPFPerformance();
					break;
				} catch (Exception e) {
					LOGGER.debug("Falla PFPerformance ({}), reintento #{}",
							e.getLocalizedMessage(), ++reintentos);
					try {
						testPFOperativo();
					} catch (SadeTesterException e1) {
						LOGGER.debug("Error al reintentar PF: {}",
								e1.getLocalizedMessage());
					}
				}

			}
		}
	}

	void testEE() {
		int reintentos = 0;
		while (true) {
			driver.get(baseURL + "/eu-web");
			if (reintentos > 2) {
				LOGGER.info("No se pudo comprobar si EE está operativo.");
				return;
			}
			try {
				testEEOperativo();
				break;
			} catch (Exception e) {
				LOGGER.debug("Falla EEOperativo ({}), reintento #{}",
						e.getLocalizedMessage(), ++reintentos);
			}

		}
		if ("perf".equalsIgnoreCase(tipo)) {
			reintentos = 0;
			while (true) {
				if (reintentos > 2) {
					LOGGER.info("No se pudo comprobar la performance de EE");
					return;
				}
				try {
					// testEEPerformance();
					testEECaratular();
					testEEPase();
					break;
				} catch (Exception e) {
					LOGGER.debug("Falla EEPerformance ({}), reintento #{}",
							e.getLocalizedMessage(), ++reintentos);
				}

			}
		}
	}

	void testGEDO() {
		int reintentos = 0;
		while (true) {
			driver.get(baseURL + "/eu-web");
			if (reintentos > 2) {
				LOGGER.info("No se pudo comprobar si GEDO está operativo.");
				return;
			}
			try {
				testGEDOOperativo();
				break;
			} catch (Exception e) {
				LOGGER.debug("Falla GEDOOperativo ({}), reintento #{}",
						e.getLocalizedMessage(), ++reintentos);
			}

		}
		if ("perf".equalsIgnoreCase(tipo)) {
			reintentos = 0;
			while (true) {
				if (reintentos > 2) {
					LOGGER.info("No se pudo comprobar la performance de GEDO");
					return;
				}
				try {
					testGEDOPerformance1();
					testGEDOPerformance2();
					testGEDOPerformance3();
					break;
				} catch (Exception e) {
					LOGGER.debug("Falla GEDOPerformance ({}), reintento #{}",
							e.getLocalizedMessage(), ++reintentos);
					try {
						testGEDOOperativo();
					} catch (SadeTesterException e1) {
						LOGGER.debug("Error al reintentar GEDO: {}",
								e1.getLocalizedMessage());
					}
				}

			}
		}
	}

	void testLOYS() {
		int reintentos = 0;
		while (true) {
			driver.get(baseURL + "/eu-web");
			if (reintentos > 2) {
				LOGGER.info("No se pudo comprobar si LOYS está operativo.");
				return;
			}
			try {
				testLOYSOperativo();
				break;
			} catch (Exception e) {
				LOGGER.debug("Falla LOYSOperativo ({}), reintento #{}",
						e.getLocalizedMessage(), ++reintentos);
			}

		}
		if ("perf".equalsIgnoreCase(tipo)) {
			reintentos = 0;
			while (true) {
				if (reintentos > 2) {
					LOGGER.info("No se pudo comprobar la performance de LOYS");
					return;
				}
				try {
					testLOYSPerformance();
					break;
				} catch (Exception e) {
					LOGGER.debug("Falla LOYSPerformance ({}), reintento #{}",
							e.getLocalizedMessage(), ++reintentos);
					try {
						testLOYSOperativo();
					} catch (SadeTesterException e1) {
						LOGGER.debug("Error al reintentar LOYS: {}",
								e1.getLocalizedMessage());
					}
				}

			}
		}
	}

	void testEEOperativo() throws SadeTesterException {
		LOGGER.debug("Verificando EE operativo...");
		// click | //tr[3]/td[2]/div/table/tbody/tr/td/table/tbody/tr/td/img |
		// By.xpath("//tr[3]/td[2]/div/table/tbody/tr/td/table/tbody/tr/td/img")
		driver.findElement(By.xpath(moduleLink("EE"))).click();
		sleep(1000);
		// assertTitle | Sistema Expediente Electrónico |
		if (assertTitle("Sistema Expediente Electrónico")) {

			LOGGER.info("Módulo EE está operativo.");

		} else {
			throw new SadeTesterException("El título de la página es "
					+ driver.getTitle());
		}
	}

	void testLOYSOperativo() throws SadeTesterException {
		LOGGER.debug("Verificando LOYS operativo...");
		// click | //tr[3]/td[2]/div/table/tbody/tr/td/table/tbody/tr/td/img |
		driver.findElement(By.xpath(moduleLink("LOYS"))).click();
		sleep(1000);
		if (assertTitle("LOyS")) {
			LOGGER.info("Módulo LOYS está operativo.");
		} else {
			throw new SadeTesterException("El título de la página es "
					+ driver.getTitle());
		}
	}

	void testTRACKOperativo() throws SadeTesterException {
		LOGGER.debug("Verificando TRACK operativo...");
		// click | //tr[4]/td[2]/div/table/tbody/tr/td/table/tbody/tr/td/img |
		driver.findElement(By.xpath(moduleLink("TRACK"))).click();
		sleep(1000);
		// assertTitle | Sistema Expediente Electrónico |
		if (assertTitle("SADE")) {
			LOGGER.info("Módulo TRACK está operativo.");
		} else {
			if (assertTitle("Sistema de Administración de Documentos Electrónicos")) {
				bypassPalomita();
			} else {
				throw new SadeTesterException("El título de la página es "
						+ driver.getTitle());
			}
		}
	}

	void testCCOOOperativo() throws SadeTesterException {
		LOGGER.debug("Verificando CCOOO operativo...");
		// click | //tr[2]/td[2]/div/table/tbody/tr/td/table/tbody/tr/td/img |
		driver.findElement(By.xpath(moduleLink("CCOO"))).click();
		sleep(1000);
		// assertTitle | Sistema Expediente Electrónico |
		if (assertTitle("Comunicaciones Oficiales")) {
			LOGGER.info("Módulo CCOOO está operativo.");
		} else {
			throw new SadeTesterException("El título de la página es "
					+ driver.getTitle());
		}
	}

	void testPFOperativo() throws SadeTesterException {
		LOGGER.debug("Verificando PF operativo...");
		// click | //tr[3]/td[2]/div/table/tbody/tr/td/table/tbody/tr/td/img |
		driver.findElement(By.xpath(moduleLink("PF"))).click();
		sleep(1000);
		// assertTitle | Sistema Expediente Electrónico |
		if (assertTitle("Sistema PORTA FIRMA")) {
			LOGGER.info("Módulo PF está operativo.");
		} else {
			throw new SadeTesterException("El título de la página es "
					+ driver.getTitle());
		}
	}

	void testGEDOOperativo() throws SadeTesterException {
		LOGGER.debug("Verificando GEDO operativo...");
		// click | //tr[3]/td[2]/div/table/tbody/tr/td/table/tbody/tr/td/img |
		driver.findElement(By.xpath(moduleLink("GEDO"))).click();
		sleep(1000);
		if (assertTitle("Sistema GEDO")) {
			LOGGER.info("Módulo GEDO está operativo.");
		} else {
			throw new SadeTesterException("El título de la página es "
					+ driver.getTitle());
		}
	}

	public void testEECaratular() throws Exception {
		// clickAt(moduleLink("EE"));
		// assertTitle(EE_TITLE);
		sw.start("EE");
		clickAt("xpath|//div[5]/div[2]/div/div/div");
		typeAndBlur("xpath|//textarea", TEST_TEXT); // Motivo interno
		typeAndBlur("xpath|//td/div/div/table/tbody/tr[2]/td[2]/div/textarea",
				TEST_TEXT); // Descripcion adicional
		typeAndBlur("xpath|//div/i/input", "RRHH00025");
		clickAt("css|img[src=\"./imagenes/Caratular.png\"]");

		String expediente = getElement("xpath|//td[3]/div/span").getText(); // Pop-up
		LOGGER.info("{}.", expediente);
		clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]");
		sw.lap("EE", expediente);
	}

	private void testEEPase() throws Exception {
		LOGGER.debug("Realizando pase");
		clickAt("xpath|//td/div/span");
		sleep(1000);
		clickAt("xpath|/html/body/div/div[2]/div/div/div[2]/div[1]/div/div/div[1]/div/div[7]/div[2]/table/tbody[2]/tr[1]/td[1]/div");
		sleep(1000);
		clickAt("xpath|/html/body/div/div[2]/div/div/div[2]/div[1]/div/div/div[1]/div/div[7]/div[2]/table/tbody[2]/tr[1]/td[9]/div/table/tbody/tr/td/table/tbody/tr/td[1]/img");
		sleep(1000);
		clickAt("css|img[src=\"./imagenes/RealizarPase.png\"]");
		sleep(1000);

		// Editor embebido
		driver.switchTo().frame(1);
		driver.switchTo().activeElement().sendKeys(TEST_TEXT);
		driver.switchTo().defaultContent();

		// Hack para HtmlUnitDriver
		// HtmlPage page = (HmtlPage) page.getFrames()[0]

		sleep(1000);
		clickAt("xpath|//div[2]/div/table/tbody/tr/td[2]/div/i/i");
		// typeAndBlur("xpath|//table/tbody/tr/td[2]/div/i/input[@tabindex=\"1\"]",
		// "G"); // Guarda Temporal
		sleep(1000);
		clickAt("xpath|//div[7]/table/tbody/tr[4]/td[2]");
		sleep(1000);
		clickAt("xpath|/html/body/div[6]/div[3]/div/div/div/div[7]");
		// clickAt("css|img[src=\"./imagenes/RealizarPase.png\"]");

		// if (isElementPresent(By.id("_z_7"))) // Error de id duplicado ZK
		// {
		// LOGGER.error( "Id duplicado en ZK");
		// driver.switchTo().activeElement().click();
		// clickAt("css|img[src=\"./imagenes/RealizarPase.png\"]");
		// if (isElementPresent(By.id("_z_8"))) // Error en el form duplicado ZK
		// {
		// LOGGER.error( "Error en form duplicado en ZK");
		// clickAt("id|_z_8-c"); // Cancelar error "falta motivo"
		// clickAt("css|img[src=\"./imagenes/RealizarPase.png\"]");
		// }
		//
		// }
		sleep(3000);
		clickAt("xpath|//div[3]/div/div/div/table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]");
		// clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]");
		sleep(1000);
		String temporal = driver.findElement(By.xpath("//td[3]/div/span"))
				.getText();
		LOGGER.debug("MSG: {}", temporal);
		clickAt("xpath|//div[3]/div/div/div/table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]"); // OK
																													// al
																													// popup
		sw.stop("EE", temporal);
	}

	void testEEPerformance() throws Exception {
		LOGGER.debug("Ejecutando operaciones EE");
		sw.start("EE");
		// driver.findElement(By.xpath("//div[5]/div[2]/div/div/div")).click();
		clickAt("xpath|//div[5]/div[2]/div/div/div");
		// type | //textarea | Test
		driver.findElement(By.xpath("//textarea")).clear();
		driver.findElement(By.xpath("//textarea")).sendKeys("Test");
		// type | //td/div/div/table/tbody/tr[2]/td[2]/div/textarea | Test
		driver.findElement(
				By.xpath("//td/div/div/table/tbody/tr[2]/td[2]/div/textarea"))
				.clear();
		driver.findElement(
				By.xpath("//td/div/div/table/tbody/tr[2]/td[2]/div/textarea"))
				.sendKeys("Test");
		// type | //div/i/input | RRHH00025
		driver.findElement(By.xpath("//div/i/input")).clear();
		driver.findElement(By.xpath("//div/i/input")).sendKeys("RRHH00025");
		driver.findElement(By.xpath("//div/i/input")).sendKeys("\t");
		driver.findElement(
				By.cssSelector("img[src=\"./imagenes/Caratular.png\"]"))
				.click();
		// storeText | //td[3]/div/span | expediente
		String expediente = driver.findElement(By.xpath("//td[3]/div/span"))
				.getText();
		sw.lap("EE", "Expediente generado " + expediente);
		LOGGER.debug("Generado el expediente {}", expediente);
		// click |
		// //table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]
		// |
		driver.findElement(
				By.xpath("//table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]"))
				.click();
		sw.lap("EE", "Confeccionado " + expediente);
		// Caratulado
		// click | //td/div/span |
		driver.findElement(By.xpath("//td/div/span")).click(); // Caratular
		// click | //td[9]/div/table/tbody/tr/td/table/tbody/tr/td/img |
		sleep(1000);
		driver.findElement(
				By.xpath("//tr[1]/td[9]/div/table/tbody/tr/td/table/tbody/tr/td[1]/img"))
				.click();
		// click |
		// //div/div/div/table/tbody/tr/td/table/tbody/tr/td[3]/div/div/div/img
		// |
		sleep(1000);
		driver.findElement(
				By.xpath("//div/div/div/table/tbody/tr/td/table/tbody/tr/td[3]/div/div/div/img"))
				.click();

		sleep(1000);
		// Editor interno
		driver.switchTo().frame(1);
		driver.switchTo().activeElement().sendKeys(TEST_TEXT);
		driver.switchTo().defaultContent();

		// click | //div[2]/div/table/tbody/tr/td[2]/div/i/input |
		driver.findElement(
				By.xpath("//div[2]/div/table/tbody/tr/td[2]/div/i/input"))
				.click();
		sleep(1000);
		// click | //div[7]/table/tbody/tr[4]/td[2] |
		driver.findElement(By.xpath("//div[7]/table/tbody/tr[4]/td[2]"))
				.click();

		// click | //div[7]/div/div/img |
		driver.findElement(By.xpath("//div[7]/div/div/img")).click();
		// storeText | //td[3]/div/span | temporal
		sleep(1000);
		getElement(
				"xpath|//div[3]/div/div/div/table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]")
				.click(); // OK al popup
		sleep(1000);
		String temporal = driver.findElement(By.xpath("//td[3]/div/span"))
				.getText();
		LOGGER.debug("MSG: {}", temporal);
		driver.findElement(
				By.xpath("//div[3]/div/div/div/table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]"))
				.click(); // OK al popup

		driver.findElement(By.xpath("//td[13]/span")).click(); // Volver al
																// escritorio
		sw.stop("EE", "Operaciones completadas");
		LOGGER.debug("Módulo EE finalizado");
	}

	public void testTRACKPerformance() throws Exception {
		LOGGER.debug("Ejecutando operaciones TRACK");

		// clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr[1]/td[1]/table/tbody/tr/td/table/tbody/tr/td[1]/a");
		LOGGER.debug("Caratular");
		sw.start("TRACK");

		clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr[1]/td[1]/table/tbody/tr/td/table/tbody/tr/td[3]/a"); // Caratular
		sleep(1000);

		// assertEquals("Búsqueda de Carátulas",
		// getElement("xpath|//*[@id='pbPrincipal']/table[1]/tbody/tr/td[2]/span").getText().trim());
		LOGGER.debug("Nuevo interno");
		clickAt("xpath|//td[4]/button");
		sleep(1000);
		// assertEquals("Caratulación de Expedientes",
		// getElement("xpath|//*[@id='pbPrincipal']/table[1]/tbody/tr/td[2]/span").getText().trim());

		typeAndBlur("xpath|//*[@id=\"body:ExtractoCaratulacionNuevo\"]",
				"SADE100");

		typeAndBlur("xpath|//textarea", TEST_TEXT);
		// waitForId("body:idDestino:_1").click(); // Seleccionar Interno
		sleep(1000);
		clickAt("xpath|//*[@id=\"body:idDestino:_1\"]");
		// waitForId("body:SectorInternoDestino_CaratulacionNuevo").sendKeys("ACSADE");
		sleep(1000);
		typeAndBlur(
				"xpath|//*[@id=\"body:SectorInternoDestino_CaratulacionNuevo\"]",
				"ACSADE");
		sleep(1000);
		typeAndBlur("xpath|//*[@id=\"body:Fojas\"]", "2");
		sleep(1000);
		typeAndBlur("xpath|//*[@id=\"body:_idJsp191\"]", TEST_TEXT);
		sleep(1000);
		clickAt("xpath|//button");
		sleep(1000);

		if (isAlertPresent()) {
			driver.switchTo().alert().accept();
		}

		if ("Expediente Generado".equals(getElement(
				"xpath|//*[@id='pbPrincipal']/table[1]/tbody/tr/td[2]/span")
				.getText().trim())) {
			String expediente = getElement(
					"xpath|//td[2]/table/tbody/tr[1]/td/span").getText();
			LOGGER.info("Expediente generado: {}", expediente);
			sw.stop("TRACK", "Expediente " + expediente);
			clickAt("xpath|//td[3]/table/tbody/tr/td[3]/a/span");
		} else {
			LOGGER.error("No generó el expediente?");
		}
	}

	private void testCCOOPerformance() throws Exception {
		LOGGER.debug("Ejecutando operaciones CCOO");
		sw.start("CCOO");
		LOGGER.debug("Inicio CO");
		clickAt("partial-link|InicioCO"); // Mis tareas (default) => Inicio CO
		sleep(1000);

		LOGGER.debug("Start Nota");
		clickAt("xpath|//tbody[@id='body:dataTipoExpediente:tbody_element']/tr[2]/td[3]/table/tbody/tr/td/a/span");
		sleep(1000);

		LOGGER.debug("Continuar tareas");
		clickAt("css|button.x8x");
		sleep(1000);

		LOGGER.debug("Seleccionar la primer tarea");
		clickAt("xpath|//tbody[@id='body:dataActividad:tbody_element']/tr/td[7]/table/tbody/tr/td[2]/a/span");
		sleep(1000);

		LOGGER.debug("Start record");
		typeAndBlur("id|body:usuarioDestino", username);
		typeAndBlur("id|body:comentario", TEST_TEXT);
		clickAt("css|button.x8w");
		sleep(1000);

		LOGGER.debug("Continuar tareas");
		clickAt("css|button.x8x");
		sleep(3000);

		LOGGER.debug("Seleccionar la primer tarea");
		clickAt("xpath|//tbody[@id='body:dataActividad:tbody_element']/tr/td[7]/table/tbody/tr/td[2]/a/span");
		sleep(1000);

		driver.switchTo().frame(1);
		((JavascriptExecutor) driver)
				.executeScript("tinyMCE.activeEditor.setContent('Test performance. Carece de motivacion administrativa.')");
		driver.switchTo().defaultContent();

		LOGGER.debug("Continuar nota");
		// clickAt("id|body:enviar");
		clickAt("xpath|//button[text()=\"Continuar Nota\"]");
		sleep(1000);

		try {
			driver.findElement(By.id("body:error"));
			driver.switchTo().frame(1);
			((JavascriptExecutor) driver)
					.executeScript("tinyMCE.activeEditor.setContent('Test performance. Carece de motivacion administrativa.')");
			driver.switchTo().defaultContent();

			LOGGER.debug("Nuevo intento de continuar nota");
			// clickAt("id|body:enviar");
			clickAt("xpath|//button[text()=\"Continuar Nota\"]");
			sleep(1000);

		} catch (Exception e) {
			// Perfecto, no hay error
		}

		LOGGER.debug("Confeccionar nota");
		typeAndBlur("id|body:destinatario", username);
		typeAndBlur("id|body:referencia", TEST_TEXT);
		typeAndBlur("id|body:descripcionTarea", TEST_TEXT);
		LOGGER.debug("Firmar y distribuir");
		clickAt("xpath|(//button[@type='button'])[6]");
		sleep(2000);

		new WebDriverWait(driver, 5).until(ExpectedConditions.alertIsPresent());

		Alert alert = driver.switchTo().alert(); // Alert = Está seguro que
													// desea firmar?
		alert.accept();
		sleep(1000);

		String co = getElement(
				"xpath|/html/body/form/table/tbody/tr/td/div/table[2]/tbody/tr/td[2]/span")
				.getText();
		LOGGER.info("Se generó la nota {}", co);
		clickAt("css|button.x8x");
		LOGGER.debug("Volviendo a bandeja de notas");
		// driver.switchTo().defaultContent();

		LOGGER.debug("Consultar comunicaciones");
		clickAt("link|Consulta de Comunicaciones Oficiales");
		sleep(1000);

		new Select(getElement("id|body:tipoActuacion")).selectByValue("NO");
		typeAndBlur("id|body:anio", "2014");
		typeAndBlur("id|body:numeroActuacion", "01079756");
		driver.findElement(By.id("body:reparticion")).sendKeys("mgeya");
		sleep(1000);
		clickAt("id|body:reparticion");
		clickAt("xpath|//li[5]");

		LOGGER.debug("Consultar");
		sw.lap("CCOO", "Consultar nota");
		clickAt("css|button.x8w");
		sleep(1000);

		String nota = getElement(
				"xpath|/html/body/form/table/tbody/tr/td/div/div[5]/table/tbody/tr/td[1]")
				.getText().trim();
		if ("NO-2014-01079756- -MGEYA".equals(nota)) {
			LOGGER.debug("Nota {} encontrada.", nota);
		} else {
			LOGGER.debug("La consulta trajo {}", nota);
		}

		sw.stop("CCOO");
	}

	private void testPFPerformance() {
		LOGGER.debug("Ejecutando operaciones PF");
		sw.start("PF");
		clickAt("xpath|//tbody[2]/tr/td/div");
		sleep(1000);
		clickAt("xpath|//div[3]/div/div/div/div");
		sleep(1000);
		clickAt("css|img[src=\"./imagenes/certificate.jpg\"]");
		sleep(1000);
		clickAt("xpath|//div[3]/span/table/tbody/tr[2]/td[2]");
		sleep(1000);
		// if(!"Firma exitosa".equals(getElement("xpath|//div[3]/div[2]/table/tbody[2]/tr/td[5]/div").getText()))
		// {
		// LOGGER.error("La firma no fue exitosa.");
		// }
		clickAt("xpath|//td[7]/span/table/tbody/tr[2]/td[2]");
		sw.stop("PF", "Firma exitosa");
		LOGGER.info("Tiempo de Firma: {} segundos.",
				sw.getElapsedTime() / 1000.0);
	}

	private void testLOYSPerformance() throws Exception {
		LOGGER.debug("Ejecutando operaciones LOYS");
		sw.start("LOYS");
//		clickAt("xpath|//td[2]/a");
		clickAt("link|Inicio LOyS");
		sleep(1000);
		typeAndBlur("xpath|//input", TEST_TEXT);

		clickAt("xpath|//tr[3]/td[2]/a/img");
		sleep(1000);
		String expediente = getElement("xpath|//div/span").getText();
		LOGGER.info("Se generó el expediente {}.", expediente);
		clickAt("xpath|//button");
		sw.stop("LOYS", "Generado expediente " + expediente);
		LOGGER.info("Tiempo de LOYS: {} segundos", sw.getElapsedTime() / 1000.0);
	}

	private void testGEDOPerformance1() throws Exception {
		LOGGER.debug("Ejecutando operaciones GEDO");
	    // Asegura que Porta Firma está activado
	    clickAt("xpath|//li[5]/div/div/div/span");
	    if (!getElement("xpath|//span/input").isSelected())
	    {
	    	clickAt("xpath|//span/input");
	    	clickAt("xpath|//div/span/table/tbody/tr[2]/td[2]");
	        clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]");
	        sleep(1000);
	    }
	    clickAt("xpath|//li/div/div/div/span");
	    sleep(1000);
	
	  // Nuevo documento texto
	    LOGGER.debug("Nuevo documento (texto)");
	    sw.start("GEDO");
	  clickAt("xpath|//div[6]/div/div/div/div");
	  sleep(1000);
	  typeAndBlur("xpath|//td[3]/i/input", "IF");
	  clickAt("css|img[src=\"/gedo-web/imagenes/ProducirloYoMismo.png\"]");
	  sleep(1000);
	
	  typeAndBlur("xpath|//td[2]/input", TEST_TEXT);
	
	  // Contenido en el Gecko editor
	  driver.switchTo().frame(1);
	  driver.switchTo().activeElement().sendKeys(TEST_TEXT);	// Editor embebido
	  driver.switchTo().defaultContent();
	
	  LOGGER.debug("Firmar yo mismo");
	  clickAt("css|img[src=\"/gedo-web/imagenes/FirmarYoMismoElDocumento.png\"]");
	  sleep(1000);
	
	  clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]");
	  LOGGER.debug("Documento (texto) enviado a Porta Firma");
	  LOGGER.info("Tiempo de firma documento texto (Porta Firma): {} segundos.", sw.getElapsedTime()/1000.0);
	  sw.lap("GEDO", "Documento texto");
	}

	private void testGEDOPerformance2() throws Exception {
	     // Nuevo documento con Gráfico
	      //TODO Resolver la copia al filesystem del gráfico a utilizar / pedirlo por parámetro?
	//      clickAt("xpath|//div[6]/div/div/div/div");
	//
	//  	  typeAndBlur("xpath|//td[3]/i/input", "IFGRA");
	//      clickAt("css|img[src=\"/gedo-web/imagenes/ProducirloYoMismo.png\"]");
	//      
	//      typeAndBlur("xpath|//td[2]/input", TEST_TEXT);
	//
	//      driver.findElement(By.name("file")).sendKeys(IMG_PATH);
	//
	//      clickAt("css|img[src=\"/gedo-web/imagenes/FirmarYoMismoElDocumento.png\"]");
	//      clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]");
	//      LOGGER.debug("Documento (gráfico) enviado a Porta Firma");
	//      sw.lap("GEDO", "Documento con gráfico");
		}

	private void testGEDOPerformance3() throws Exception {
	      // Nuevo documento SIN enviar a Porta Firma
	      // Desactivar Portafirma
			      LOGGER.debug("Desactivar Portafirma");
	      clickAt("xpath|//li[5]/div/div/div/span");
	      sleep(1000);
	      clickAt("xpath|//span/input");
	      clickAt("xpath|//div/span/table/tbody/tr[2]/td[2]");
	      sleep(1000);
	      clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]");
	      sleep(1000);
	      clickAt("xpath|//li/div/div/div/span"); // Volver a Mis Tareas
	//      clickAt(waitForXpath("xpath|//div[5]"), "???");  // Click espúreo?
	      
	      LOGGER.debug("Iniciar documento (texto) para firmar con certificado");
	      sw.lap("GEDO", "Iniciar documento texto sin portafirma");
		  clickAt("xpath|//div[6]/div/div/div/div");
	  	  typeAndBlur("xpath|//td[3]/i/input", "IF");
	      LOGGER.debug("Producirlo yo mismo");
	  	  clickAt("css|img[src=\"/gedo-web/imagenes/ProducirloYoMismo.png\"]");
	      sleep(1000);
	
	      typeAndBlur("xpath|//td[2]/input", TEST_TEXT);
	      
	      // Contenido en el Gecko editor
	      driver.switchTo().frame(1);
	      driver.switchTo().activeElement().sendKeys(TEST_TEXT);
	      driver.switchTo().defaultContent();
	      
	      LOGGER.debug("Firmarlo yo mismo");
	      clickAt("css|img[src=\"/gedo-web/imagenes/FirmarYoMismoElDocumento.png\"]");
	      sleep(1000);
	
	      LOGGER.debug("Firmar con certificado");
	      clickAt("css|img[src=\"./imagenes/FirmarConCertificado.png\"]");
	      sleep(1000);
	      
	      
		String expediente = 
		  		getElement("xpath|//div[2]/div/div/div/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/span").getText();
		  LOGGER.info( "Generado el expediente {}.", expediente);
	      clickAt("xpath|//td[2]/div/div/div/div/img");
	      sleep(1000);
	
	      LOGGER.debug("Reponer Portafirma");
	      clickAt("xpath|//li[5]/div/div/div/span");
	      sleep(1000);
	      clickAt("xpath|//span/input");
	      sleep(1000);
	      clickAt("xpath|//div/span/table/tbody/tr[2]/td[2]");
	      sleep(1000);
	      clickAt("xpath|//table[2]/tbody/tr/td/table/tbody/tr/td/span/table/tbody/tr[2]/td[2]");
	      sleep(1000);
	      LOGGER.info("Tiempo de firma documento texto (Certificado): {} segundos.", sw.getElapsedTime()/1000.0);
	      sw.stop("GEDO", "Documento texto SIN portafirma: " + expediente);
		}

	void close() {
		driver.quit();
		LOGGER.debug("Driver cerrado, logback fuera.");
		LOGGER.info("\nSADE Tester finalizado.");
		LoggerContext loggerContext = (LoggerContext) LoggerFactory
				.getILoggerFactory();
		loggerContext.stop();
	}

	private void bypassPalomita() throws SadeTesterException {
		driver.findElement(By.xpath("//input")).clear();
		driver.findElement(By.xpath("//input")).sendKeys(username);
		driver.findElement(By.xpath("//tr[4]/td[2]/input")).clear();
		driver.findElement(By.xpath("//tr[4]/td[2]/input")).sendKeys(password);
		driver.findElement(By.xpath("//tr[5]/td/input")).click();
		if (assertTitle("Página de control de error generico.")) {
			driver.get(baseURL + "/sade-satra-tramita-web/sade");
			if (!assertTitle("SADE")) {
				throw new SadeTesterException("El título de la página es "
						+ driver.getTitle());
			}
		}
	}

	void parseArguments(String[] args) {

		if (args == null || args.length == 0 || args[0].contains("-h")) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("sadetester", options, true);
			System.exit(0);
		} else {
			CommandLineParser parser = new BasicParser();
			try {
				CommandLine cmd = parser.parse(options, args, true);
				if (cmd.hasOption("t")) {
					tipo = cmd.getOptionValue("t");
				}
				if (cmd.hasOption("a")) {
					ambiente = cmd.getOptionValue("a").toLowerCase().trim();
					if (!"dev".equals(ambiente) && !"prd".equals(ambiente)
							&& !"hml".equals(ambiente)) {
						System.out.println("El ambiente " + ambiente
								+ " no parece ser válido.");
						System.exit(0);
					}
				}
				if (cmd.hasOption("m")) {
					String codigo = cmd.getOptionValue("m").toUpperCase();
					modulo = ModuloSADE.valueOf(codigo);
				}
				if (cmd.hasOption("u")) {
					username = cmd.getOptionValue("u");
				}
				if (cmd.hasOption("p")) {
					password = cmd.getOptionValue("p");
				}
				if (cmd.hasOption("d")) {
					debug = true;
				}
				LOGGER.debug(
						"sadetest tipo={}, ambiente={}, modulo={}, debug={}, user={}, pass={}",
						tipo, ambiente, modulo, debug, username, password);
			} catch (MissingOptionException moe) {
				System.out.println("Falta algún parámetro requerido.\n"
						+ moe.getLocalizedMessage());
				HelpFormatter helpFormatter = new HelpFormatter();
				helpFormatter.printHelp("sadetester", options, true);
				System.exit(0);
			} catch (ParseException e) {
				HelpFormatter helpFormatter = new HelpFormatter();
				helpFormatter.printHelp("sadetester", options, true);
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	public SadeTester(String[] args) {
		buildOptions();
		parseArguments(args);
	}

	@SuppressWarnings("static-access")
	static void buildOptions() {

		options.addOption(OptionBuilder.withArgName("perf|oper")
				.isRequired(true).hasArg(true).withLongOpt("tipo")
				.withDescription("tipo de test a realizar").create("t"));
		options.addOption(OptionBuilder.withArgName("env|hml|prd")
				.isRequired(true).hasArg(true).withLongOpt("ambiente")
				.withDescription("ambiente donde realizar el test").create("a"));
		options.addOption(OptionBuilder.withArgName("string").isRequired(true)
				.hasArg(true).withLongOpt("usuario")
				.withDescription("usuario para el login a SADE").create("u"));
		options.addOption(OptionBuilder.withArgName("string").isRequired(true)
				.hasArg(true).withLongOpt("password")
				.withDescription("password para el login a SADE").create("p"));
		options.addOption(OptionBuilder
				.withLongOpt("modulo")
				.hasArg(true)
				.withArgName("gedo|ccoo|...")
				.isRequired(false)
				.withDescription(
						"modulo a testear (default = todo el ambiente)")
				.create("m"));
		options.addOption(OptionBuilder.withLongOpt("debug").isRequired(false)
				.hasArg(false)
				.withDescription("imprime información adicional para debug")
				.create("d"));
		options.addOption(OptionBuilder.withLongOpt("help").isRequired(false)
				.hasArg(false).withDescription("imprime esta ayuda")
				.create("h"));
	}

	public String getAmbiente() {
		return ambiente;
	}

	public ModuloSADE getModulo() {
		return modulo;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDriver(final WebDriver theDriver) {
		LOGGER.debug("Inyectando driver {}.", theDriver.getClass());
		this.driver = theDriver;
	}

	String moduleLink(final String code) {
		return "//table/tbody[2]/tr[./td/div/span[text() = '" + code
				+ "']]/td/div/table/tbody/tr/td/table/tbody/tr/td/img";
	}

	public static String getTipo() {
		return tipo;
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

	private void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// OK to ignore
		}
	}

	boolean assertTitle(final String title) {
		try {
			return new FluentWait<WebDriver>(driver)
					.pollingEvery(3, TimeUnit.SECONDS)
					.withTimeout(10, TimeUnit.SECONDS)
					.until(ExpectedConditions.titleIs(title));
		} catch (Exception e) {
			return false;
		}
	}

	private WebElement getElement(final String path) throws Exception {
		By locator = buildLocator(path);
		final long start = System.currentTimeMillis();
		Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
				.ignoring(StaleElementReferenceException.class)
				.pollingEvery(10, TimeUnit.SECONDS)
				.withTimeout(45, TimeUnit.SECONDS);

		WebElement element = null;

		while ((System.currentTimeMillis() - start) < 91000) {
			try {
				element = wait.until(ExpectedConditions
						.presenceOfElementLocated(locator));
				LOGGER.debug("Encontrado {} despues de {} mS.", path,
						(System.currentTimeMillis() - start));
				return element;
			} catch (StaleElementReferenceException sere) {
				LOGGER.debug("Stale: {}. Reintento.", sere.getMessage());
			}
		}
		System.out.println(driver.getPageSource());
		throw new Exception("Elemento " + locator + " no encontrado.");

	}

	private By buildLocator(final String path) {
		String[] p = path.split("[|]");
		By locator;
		if ("css".equals(p[0])) {
			locator = By.cssSelector(p[1]);
		} else if ("xpath".equals(p[0])) {
			locator = By.xpath(p[1]);
		} else if ("id".equals(p[0])) {
			locator = By.id(p[1]);
		} else if ("link".equals(p[0])) {
			locator = By.linkText(p[1]);
		} else if ("partial-link".equals(p[0])) {
			locator = By.partialLinkText(p[1]);
		} else {
			LOGGER.error(
					"El tipo de selector {} del elemento {} no es válido.",
					p[0], path);
			throw new IllegalStateException(
					"El path del elemento es inválido. Ver el log.");
		}
		return locator;
	}

	private void typeAndBlur(String path, String text) throws Exception {
		WebElement field = null;
		field = getElement(path);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].focus(); return true", field);
		field.sendKeys(text);
		js.executeScript("arguments[0].blur(); return true", field);

		// field.sendKeys(Keys.TAB); // Fire change event (just in case)

	}

	private void clickAt(String path) {
		for (int i = 0; i < 5; i++) {
			try {
				WebElement element = getElement(path);
				element.click();
				break;
			} catch (Exception e) {
				LOGGER.error("No pudo clickear en {}, reintento #{}.", path, i);
			}
		}
	}

	public static class ConsoleFilter extends Filter<ILoggingEvent> {

		@Override
		public FilterReply decide(ILoggingEvent event) {
			if (event.getLevel() == Level.DEBUG) {
				return (debug) ? FilterReply.ACCEPT : FilterReply.DENY;
			}
			return FilterReply.NEUTRAL;
		}

	}
}
