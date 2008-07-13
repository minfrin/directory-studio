/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.studio.test.integration.ui;


import net.sf.swtbot.eclipse.finder.SWTEclipseBot;
import net.sf.swtbot.finder.UIThreadRunnable;
import net.sf.swtbot.finder.UIThreadRunnable.IntResult;
import net.sf.swtbot.wait.DefaultCondition;
import net.sf.swtbot.widgets.SWTBotCombo;
import net.sf.swtbot.widgets.SWTBotMenu;
import net.sf.swtbot.widgets.SWTBotText;
import net.sf.swtbot.widgets.SWTBotTree;
import net.sf.swtbot.widgets.SWTBotTreeItem;

import org.apache.directory.server.unit.AbstractServerTest;
import org.eclipse.swt.graphics.DeviceData;


/**
 * Tests allocation of SWT Resources.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SwtResourcesTest extends AbstractServerTest
{
    private SWTEclipseBot bot;


    protected void setUp() throws Exception
    {
        super.setUp();
        bot = new SWTEclipseBot();
        SWTBotUtils.openLdapPerspective( bot );
        SWTBotUtils.openSleakView( bot );
        SWTBotUtils.createTestConnection( bot, "SwtResourcesTest", ldapServer.getIpPort() );
    }


    protected void tearDown() throws Exception
    {
        SWTBotUtils.deleteTestConnections();
        bot = null;
        super.tearDown();
    }


    /**
     * Test for DIRSTUDIO-319.
     * 
     * Creates multiple entries using the New Entry wizard. Checks
     * that we don't allocate too much SWT resources during the run.
     * 
     * @throws Exception the exception
     */
    public void testSwtResourcesDelta() throws Exception
    {
        SWTBotTree browserTree = SWTBotUtils.getLdapBrowserTree( bot );

        // run the new entry wizard once to ensure all SWT resources are created
        createAndDeleteEntry( browserTree, "testSwtResourcesDelta" + 0 );

        // remember the SWT objects before the run
        int beforeObjectCount = getSwtObjectCount();

        // now lets run the new entry wizard it several times
        for ( int i = 1; i < 25; i++ )
        {
            createAndDeleteEntry( browserTree, "testSwtResourcesDelta" + i );
        }

        // get the SWT objects after the run
        int afterObjectCount = getSwtObjectCount();

        // we expect not more than 10 additional SWT objects
        assertTrue( "Too many SWT resources were allocated in testSwtResourcesDelta: before=" + beforeObjectCount
            + ", after=" + afterObjectCount, afterObjectCount - beforeObjectCount < 10 );
    }


    /**
     * Ensure that we have not allocated more the 1000 SWT resources 
     * during the complete test suite.
     * 
     * 1000 is not a fix number but it is a good starting point.
     * 
     * @throws Exception the exception
     */
    public void testSwtResourcesCount() throws Exception
    {
        int swtObjectCount = getSwtObjectCount();
        assertTrue( "Too many SWT resources were allocated: " + swtObjectCount, swtObjectCount < 1000 );
    }


    private int getSwtObjectCount()
    {
        return UIThreadRunnable.syncExec( bot.getDisplay(), new IntResult()
        {
            public int run()
            {
                DeviceData info = bot.getDisplay().getDeviceData();
                if ( !info.tracking )
                {
                    fail( "To run this test options 'org.eclipse.ui/debug' and 'org.eclipse.ui/trace/graphics' must be true." );
                }
                return info.objects.length;
            }
        } );
    }


    private void createAndDeleteEntry( final SWTBotTree browserTree, final String name ) throws Exception
    {
        SWTBotTreeItem systemNode = SWTBotUtils.selectNode( bot, browserTree, "DIT", "Root DSE", "ou=system" );
        systemNode.expand();
        systemNode.expand();

        //        bot.sleep( 1000 );
        SWTBotMenu contextMenu = browserTree.contextMenu( "New Entry..." );
        contextMenu.click();

        bot.radio( "Create entry from scratch" ).click();
        bot.button( "Next >" ).click();

        bot.table( 0 ).select( "organization" );
        bot.button( "Add" ).click();
        bot.button( "Next >" ).click();

        SWTBotCombo typeCombo = bot.comboBoxWithLabel( "RDN:" );
        typeCombo.setText( "o" );
        SWTBotText valueText = bot.text( "" );
        valueText.setText( name );
        bot.button( "Next >" ).click();

        // wait for check that entry doesn't exist yet
        bot.waitUntil( new DefaultCondition()
        {
            public boolean test() throws Exception
            {
                return bot.button( "Finish" ).isEnabled();
            }


            public String getFailureMessage()
            {
                return "Finish button is not enabled";
            }
        } );
        bot.button( "Finish" ).click();

        // wait till entry is created and selected in the tree
        bot.waitUntil( new DefaultCondition()
        {
            public boolean test() throws Exception
            {
                return browserTree.selection().get( 0 ).get( 0 ).startsWith( "o=" + name );
            }


            public String getFailureMessage()
            {
                return "Could not select 'o=" + name + "'";
            }
        } );

        // delete the entry
        SWTBotUtils.selectNode( bot, browserTree, "DIT", "Root DSE", "ou=system", "o=" + name );
        contextMenu = browserTree.contextMenu( "Delete Entry" );
        contextMenu.click();
        bot.button( "OK" ).click();

        // wait till the parent ou=system is selected in the tree
        bot.waitUntil( new DefaultCondition()
        {
            public boolean test() throws Exception
            {
                return browserTree.selection().get( 0 ).get( 0 ).startsWith( "ou=system" );
            }


            public String getFailureMessage()
            {
                return "Could not select 'ou=system'";
            }
        } );
    }

}
