/*
 * Copyright (c) 2002-2016, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.grusupply.business.daemon;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * This class provides Data Access methods for Indexer Action objects
 */
public final class IndexerActionDAO implements IIndexerActionDAO
{
    //Constants
    public static final String CONSTANT_WHERE = " WHERE ";
    public static final String CONSTANT_AND = " AND ";

    // Constants
    private static final String SQL_QUERY_SELECT = "SELECT id_action,id_customer,id_task" +
        " FROM grusupply_customer_indexer_action  ";
    private static final String SQL_QUERY_DELETE = "DELETE FROM grusupply_customer_indexer_action WHERE id_action = ? ";
    private static final String SQL_FILTER_ID_TASK = " id_task = ? ";
    private static final String SQL_FILTER_ID_CUSTOMER = " id_customer = ? ";

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nId, Plugin plugin )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin );
        daoUtil.setInt( 1, nId );
        daoUtil.executeUpdate(  );
        daoUtil.free(  );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndexerAction> selectList( IndexerActionFilter filter, Plugin plugin )
    {
        List<IndexerAction> indexerActionList = new ArrayList<IndexerAction>(  );
        IndexerAction indexerAction = null;
        List<String> listStrFilter = new ArrayList<String>(  );

        if ( filter.containsTask(  ) )
        {
            listStrFilter.add( SQL_FILTER_ID_TASK );
        }

        if ( filter.containsIdCustomer(  ) )
        {
            listStrFilter.add( SQL_FILTER_ID_CUSTOMER );
        }

        String strSQL = buildRequestWithFilter( SQL_QUERY_SELECT, listStrFilter, null );

        DAOUtil daoUtil = new DAOUtil( strSQL, plugin );

        int nIndex = 1;

        if ( filter.containsTask(  ) )
        {
            daoUtil.setInt( nIndex, filter.getIndexerTask(  ).getValue(  ) );
            nIndex++;
        }

        if ( filter.containsIdCustomer(  ) )
        {
            daoUtil.setInt( nIndex, filter.getIdCustomer(  ) );
        }

        daoUtil.executeQuery(  );

        while ( daoUtil.next(  ) )
        {
            indexerAction = new IndexerAction(  );
            indexerAction.setIdAction( daoUtil.getInt( 1 ) );
            indexerAction.setIdTicket( daoUtil.getInt( 2 ) );
            indexerAction.setTask( IndexerTask.valueOf( daoUtil.getInt( 3 ) ) );

            indexerActionList.add( indexerAction );
        }

        daoUtil.free(  );

        return indexerActionList;
    }

    /**
     * Builds a query with filters placed in parameters
     * @param strSelect the select of the query
     * @param listStrFilter the list of filter to add in the query
     * @param strOrder the order by of the query
     * @return a query
     */
    private static String buildRequestWithFilter( String strSelect, List<String> listStrFilter, String strOrder )
    {
        StringBuffer strBuffer = new StringBuffer(  );
        strBuffer.append( strSelect );

        int nCount = 0;

        for ( String strFilter : listStrFilter )
        {
            if ( ++nCount == 1 )
            {
                strBuffer.append( CONSTANT_WHERE );
            }

            strBuffer.append( strFilter );

            if ( nCount != listStrFilter.size(  ) )
            {
                strBuffer.append( CONSTANT_AND );
            }
        }

        if ( strOrder != null )
        {
            strBuffer.append( strOrder );
        }

        return strBuffer.toString(  );
    }
}