/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.probe.ftest;

import static junit.framework.Assert.assertTrue;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardNoRequest;
import static org.jboss.arquillian.graphene.Graphene.waitAjax;
import static org.jboss.arquillian.graphene.Graphene.waitModel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Reception;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Model;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.probe.InvocationMonitor;
import org.jboss.weld.probe.ProbeFilter;
import org.jboss.weld.probe.tests.integration.JSONTestUtil;
import org.jboss.weld.probe.tests.integration.ProbeBeansTest;
import org.jboss.weld.probe.tests.integration.deployment.InvokingServlet;
import org.jboss.weld.probe.tests.integration.deployment.annotations.Collector;
import org.jboss.weld.probe.tests.integration.deployment.beans.ApplicationScopedObserver;
import org.jboss.weld.probe.tests.integration.deployment.beans.ModelBean;
import org.jboss.weld.probe.tests.integration.deployment.beans.SessionScopedBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Tomas Remes
 * @author Matej Novotny
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ProbeFunctionalTest {

    protected static final String PROBE = "weld-probe";
    protected static final String ARCHIVE_NAME = "probe-ftest";
    protected static final String SERVLET_INVOKED = "GET /probe-ftest/test";

    @Drone
    WebDriver driver;

    @ArquillianResource
    private URL contextPath;

    @Page
    private PageFragment page;
    
    @FindBy(className = "form-control-static")
    List<WebElement> listOfTargetElements;
    
    @Deployment(testable = false)
    public static WebArchive createTestDeployment1() {
        WebArchive webArchive = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war")
                .addAsWebInfResource(ProbeBeansTest.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(ProbeBeansTest.class.getPackage(), "beans.xml", "beans.xml")
                .addPackage(ModelBean.class.getPackage())
                .addPackage(Collector.class.getPackage())
                .addClass(InvokingServlet.class);
        return webArchive;
    }

    @Before
    public void openStartUrl() throws MalformedURLException {
        // By default you land on Dashboard tab
        driver.navigate().to(new URL(contextPath.toString() + PROBE));
        waitModel().until().element(page.getBeanArchivesTab()).is().present();
    }

    @Test
    public void testBeanArchiveDetail() {
        page.getBeanArchivesTab().click();
        waitAjax(driver).until().element(By.partialLinkText(ARCHIVE_NAME)).is().visible();
        guardNoRequest(driver.findElement(By.partialLinkText(ARCHIVE_NAME))).click();
        assertTrue(checkListContainsElementWithText(listOfTargetElements, BeanDiscoveryMode.ALL.name()));
        assertTrue(checkListContainsElementWithPartialText(listOfTargetElements, InvocationMonitor.class.getName()));
        assertTrue(checkListContainsElementWithPartialText(listOfTargetElements, ARCHIVE_NAME));
    }

    @Test
    public void testBeanDetail() {
        guardAjax(page.getBeansTab()).click();
        String className = ModelBean.class.getSimpleName();
        waitAjax(driver).until().element(By.partialLinkText(className)).is().visible();
        WebElement modelBeanLink = driver.findElement(By.partialLinkText(className));
        assertTrue("Cannot find element for " + className, modelBeanLink.isDisplayed());
        guardAjax(modelBeanLink).click();
        assertTrue(checkListContainsElementWithText(listOfTargetElements, ModelBean.class.getName()));
        assertTrue(checkListContainsElementWithText(listOfTargetElements, "@" + RequestScoped.class.getSimpleName()));
        assertTrue(checkListContainsElementWithText(listOfTargetElements, JSONTestUtil.BeanType.MANAGED.name()));
        assertTrue(checkListContainsElementWithText(listOfTargetElements, Model.class.getName()));
    }

    @Test
    public void testObserverMethodDetail() {
        page.getObserversTab().click();
        waitAjax().until().element(By.partialLinkText("Observer Methods")).is().visible();
        WebElement observerLink = driver.findElement(By.partialLinkText(ApplicationScopedObserver.class.getSimpleName()));
        assertTrue("Cannot find element for " + ApplicationScopedObserver.class.getSimpleName(), observerLink.isDisplayed());
        guardAjax(observerLink).click();
        assertTrue(checkListContainsElementWithText(listOfTargetElements, ApplicationScopedObserver.class.getName()));
        assertTrue(checkListContainsElementWithText(listOfTargetElements, "@" + ApplicationScoped.class.getSimpleName()));
        assertTrue(checkListContainsElementWithText(listOfTargetElements, JSONTestUtil.BeanType.MANAGED.name()));
        assertTrue(checkListContainsElementWithText(listOfTargetElements, "@" + Default.class.getSimpleName()));
        assertTrue(checkListContainsElementWithPartialText(listOfTargetElements, Reception.ALWAYS.name()));
        assertTrue(checkListContainsElementWithPartialText(listOfTargetElements, Reception.IF_EXISTS.name()));
    }

    @Test
    public void testMonitoringSessionScopeContext() throws MalformedURLException {
        invokeServletAndReturnToProbeClient();

        page.getMonitoringTab().click();
        waitAjax(driver).until().element(page.getSessionScopedContext()).is().visible();
        guardAjax(page.getSessionScopedContext()).click();
        WebElement sesionScopedBean = driver.findElement(By.partialLinkText(SessionScopedBean.class.getSimpleName()));
        assertTrue("Cannot find element for " + SessionScopedBean.class.getSimpleName(), sesionScopedBean.isDisplayed());
    }

    @Test
    public void testMonitoringInvocationTree() throws MalformedURLException {
        invokeServletAndReturnToProbeClient();
        
        page.getMonitoringTab().click();
        waitAjax(driver).until().element(page.getInvocationTrees()).is().visible();
        guardAjax(page.getInvocationTrees()).click();
        List<WebElement> invocationTableValues = driver.findElements(By.tagName("td"));
        assertTrue(checkListContainsElementWithText(invocationTableValues, ProbeFilter.class.getName()));
        assertTrue(checkListContainsElementWithText(invocationTableValues, SERVLET_INVOKED));
    }

    private void invokeServletAndReturnToProbeClient() throws MalformedURLException {
        driver.navigate().to(new URL(contextPath.toString() + "test"));
        driver.navigate().to(new URL(contextPath.toString() + PROBE));
        waitModel().until().element(page.getBeanArchivesTab()).is().present();
    }

    private boolean checkListContainsElementWithText(List<WebElement> elements, String text) {
        boolean contains = false;
        for (WebElement element : elements) {
            if (element.getText().equals(text)) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    private boolean checkListContainsElementWithPartialText(List<WebElement> elements, String text) {
        boolean contains = false;
        for (WebElement element : elements) {
            if (element.getText().contains(text)) {
                contains = true;
                break;
            }
        }
        return contains;
    }
}