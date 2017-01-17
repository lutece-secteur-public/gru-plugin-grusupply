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
package fr.paris.lutece.plugins.grusupply.service;

import fr.paris.lutece.plugins.grubusiness.business.customer.Customer;
import fr.paris.lutece.plugins.grubusiness.business.demand.Demand;
import fr.paris.lutece.plugins.grubusiness.business.indexing.IIndexingService;
import fr.paris.lutece.plugins.grubusiness.business.indexing.IndexingException;
import fr.paris.lutece.portal.service.spring.SpringContextService;


/**
 * This class represents a service for indexing
 */
public final class IndexService
{
    private static final String BEAN_CUSTOMER_INDEX_SERVICE = "grusupply.customerIndexService";
    private static final String BEAN_DEMAND_INDEX_SERVICE = "grusupply.demandIndexService";
    private static IndexService _singleton;
    private static IIndexingService<Customer> _customerIndexingService;
    private static IIndexingService<Demand> _demandIndexingService;

    /** private constructor */
    private IndexService(  )
    {
    }

    /**
     * Returns the unique instance
     * @return The unique instance
     */
    public static IndexService instance(  )
    {
        if ( _singleton == null )
        {
            _singleton = new IndexService(  );
            _customerIndexingService = SpringContextService.getBean( BEAN_CUSTOMER_INDEX_SERVICE );
            _demandIndexingService = SpringContextService.getBean( BEAN_DEMAND_INDEX_SERVICE );
        }

        return _singleton;
    }

    /**
     * Indexes the customer
     *
     * @param customer The customer
     */
    public void index( Customer customer ) throws IndexingException
    {
        _customerIndexingService.index( customer );
    }

    /**
     * Indexes the demand
     *
     * @param demand The demand
     * @param customer the customer
     */
    public void index( Demand demand ) throws IndexingException
    {
        _demandIndexingService.index( demand );
    }
}