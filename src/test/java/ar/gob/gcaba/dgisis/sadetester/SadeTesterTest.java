package ar.gob.gcaba.dgisis.sadetester;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.By.ByCssSelector;
import org.openqa.selenium.By.ById;
import org.openqa.selenium.By.ByName;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebElement;

public class SadeTesterTest {

	SadeTester sut;
	
	@Mock
	WebDriver mockDriver;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(mockDriver.getTitle()).thenReturn("SADE");
		when(mockDriver.manage()).thenReturn(mock(Options.class));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLogin() throws Exception {

		WebElement mockText = mock(WebElement.class);
		WebElement mockButton = mock(WebElement.class);
		By idLocator = ById.id(anyString());
		By name = ByName.name("submit");
		when(mockDriver.findElement(idLocator)).thenReturn(mockText);
		when(mockDriver.findElement(name)).thenReturn(mockButton);
		when(mockDriver.getTitle()).thenReturn("Sistema de Administración de Documentos Electrónicos");
		sut = new SadeTester(new String[] {"-t", "perf", "-a", "hml","--usuario", "username", "--password", "pwd"});
		sut.setDriver( mockDriver );
		sut.setup();
		when(mockDriver.getTitle()).thenReturn("Escritorio Único");
		sut.login();
		verify(mockDriver, times(2)).findElement(By.id("username"));
		verify(mockDriver, times(2)).findElement(By.id("password"));
		verify(mockDriver).findElement(By.name("submit"));
		verify(mockText, times(2)).sendKeys(anyString());
		verify(mockButton).click();
		
	}
	
	@Test
	public void testGotoBaseURL() throws Exception {
		when(mockDriver.getTitle()).thenReturn("Sistema de Administración de Documentos Electrónicos");
		sut = new SadeTester(new String[] {"-t", "perf", "-a", "dev","--usuario", "username", "--password", "pwd"});
		sut.setDriver( mockDriver );
		sut.setup();
		verify(mockDriver).get("http://eu.dev.gcba.gob.ar/");
	}

	@Test
	public void testTipoPerfFull() throws Exception {
		when(mockDriver.getTitle()).thenReturn("Sistema de Administración de Documentos Electrónicos");
		sut = new SadeTester(new String[] {"-t", "perf", "-a", "dev","--usuario", "username", "--password", "pwd"});
		sut.setDriver( mockDriver );		
		sut.setup();
		sut.run();
		verify(mockDriver).get("http://eu.dev.gcba.gob.ar/");
	}

	@Test
	public void testTipoOperfFull() throws Exception {
		when(mockDriver.getTitle()).thenReturn("Sistema de Administración de Documentos Electrónicos");
		sut = new SadeTester(new String[] {"-t", "oper", "-a", "dev","--usuario", "username", "--password", "pwd"});
		sut.setDriver( mockDriver );		
		sut.setup();
		sut.run();
		verify(mockDriver).get("http://eu.dev.gcba.gob.ar/");
	}

	@Test
	public void testArgumentParserAmbiente() {
		sut = new SadeTester(new String[] {"-t", "perf", "-a", "hml","--usuario", "username", "--password", "pwd"});
		assertEquals("hml", sut.getAmbiente());
	}

	@Test
	public void testArgumentParserModulo() {
		sut = new SadeTester(new String[] {"-m", "gedo", "-t", "perf", "-a", "hml","--usuario", "username", "--password", "pwd"});
		assertEquals(ModuloSADE.GEDO, sut.getModulo());
	}

	@Test
	public void testArgumentParserUsuario() {
		sut = new SadeTester(new String[] {"-t", "perf", "-a", "hml","--usuario", "username", "--password", "pwd"});
		assertEquals("username", sut.getUsername());
	}

	@Test
	public void testArgumentParserPassword() {
		sut = new SadeTester(new String[] {"-t", "perf", "-a", "hml","--usuario", "username", "--password", "pwd"});
		assertEquals("pwd", sut.getPassword());
	}

	@Test
	public void testArgumentParserDebug() {
		sut = new SadeTester(new String[] {"-d", "-t", "perf", "-a", "hml","--usuario", "username", "--password", "pwd"});
		assertTrue(sut.isDebug());
	}

	@Test
	public void testTestEEPerformance() throws Exception {
		fail("Not yet implemented");
	}

}
