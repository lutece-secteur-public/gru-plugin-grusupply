/*
 * Copyright (c) 2002-2015, Mairie de Paris
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
package fr.paris.lutece.plugins.grusupply.web.rs;

import com.mysql.jdbc.StringUtils;

import fr.paris.lutece.plugins.grusupply.business.Customer;
import fr.paris.lutece.plugins.grusupply.business.Demand;
import fr.paris.lutece.plugins.grusupply.business.Notification;
import fr.paris.lutece.plugins.grusupply.business.dto.NotificationDTO;
import fr.paris.lutece.plugins.grusupply.business.dto.UserDTO;
import fr.paris.lutece.plugins.grusupply.constant.GruSupplyConstants;
import fr.paris.lutece.plugins.grusupply.service.CustomerService;
import fr.paris.lutece.plugins.grusupply.service.StorageService;
import fr.paris.lutece.plugins.grusupply.service.UserInfoService;
import fr.paris.lutece.plugins.rest.service.RestConstants;
import fr.paris.lutece.portal.service.util.AppLogService;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path( RestConstants.BASE_PATH + GruSupplyConstants.PLUGIN_NAME )
public class GRUSupplyRestService
{
    private static final String STATUS_RECEIVED = "{ \"status\": \"received\" }";

    /**
     * Web Service methode which permit to store the notification flow into a data store
     * @param strJson The JSON flow
     * @return The response
     */
    @POST
    @Path( "notification" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response notification( String strJson )
    {
        try
        {
            // Format from JSON
            ObjectMapper mapper = new ObjectMapper(  );
            mapper.configure( Feature.UNWRAP_ROOT_VALUE, true );

            NotificationDTO notif = mapper.readValue( strJson, NotificationDTO.class );
            AppLogService.info( "grusupply - Received strJson : " + strJson );

            // Find CID in GRU Database
            fr.paris.lutece.plugins.gru.business.customer.Customer gruCustomer;
            
            String strTempCid = notif.getCustomerid(  );
            String strTempGuid = notif.getUserGuid(  );

            // CASE 1 NOT CID
            if ( StringUtils.isNullOrEmpty( strTempCid ) )
            {
                // CASE 1.1 : no cid and no guid:  break the flux and wait for a new flux with one of them
                if ( StringUtils.isNullOrEmpty( strTempGuid ) )
                {
                    return error( "grusupply - Error : JSON doesnot contains any GUID nor Customer ID" );
                } // CASE 1.2  : no cid and guid:  look for a mapping beween an existing guid
                else
                {
                	gruCustomer = CustomerService.instance( ).getCustomerByGuid( notif.getUserGuid(  ) );

                    if ( gruCustomer == null )
                    {
                        gruCustomer = CustomerService.instance().createCustomer( buildCustomer( UserInfoService.instance(  ).getUserInfo( strTempGuid ), strTempGuid ) );
                        AppLogService.info( "New user created into the GRU for the guid : " + strTempGuid + " its customer id is : " + gruCustomer.getId(  ) );
                    }
                }
            } // CASE 2 : cid and (guid or no guid):  find customer info in GRU database
            else
            {

                gruCustomer = CustomerService.instance(  ).getCustomerByCid( strTempCid );
                if ( gruCustomer == null )
                {
                    return error( "grusupply - Error : No user found with the customer ID : " + strTempCid );
                }
            }

            Customer user = buildCustomer( gruCustomer );
            Demand demand = buildDemand( notif, user );
            Notification notification = buildNotif( notif, demand, strJson );

            // Parse to Customer (TODO HAVE TO ADD WITH OPENAM)
            StorageService.instance(  ).store( user );

            // Parse to Demand
            StorageService.instance(  ).store( demand );

            // Parse to Notification
            StorageService.instance(  ).store( notification );
        }
        catch ( JsonParseException ex )
        {
            return error( ex + " :" + ex.getMessage(  ), ex );
        }
        catch ( JsonMappingException ex )
        {
            return error( ex + " :" + ex.getMessage(  ), ex );
        }
        catch ( IOException ex )
        {
            return error( ex + " :" + ex.getMessage(  ), ex );
        }

        return Response.status( Response.Status.CREATED ).entity( STATUS_RECEIVED ).build(  );
    }
    /**
     * Methode which create a gru Customer
     * @param user User from SSO database
     * @param strUserId ID from Flux
     * @return the Customer
     */
    private static fr.paris.lutece.plugins.gru.business.customer.Customer buildCustomer( UserDTO user , String strUserId )
    {
		fr.paris.lutece.plugins.gru.business.customer.Customer gruCustomer = new fr.paris.lutece.plugins.gru.business.customer.Customer(  );
		gruCustomer.setFirstname( setEmptyValueWhenNullValue(user.getFirstname(  ) ));
		gruCustomer.setLastname( setEmptyValueWhenNullValue(user.getLastname(  ) ));
		gruCustomer.setEmail( setEmptyValueWhenNullValue(user.getEmail(  )) );
		//gruCustomer.setAccountGuid( user.getUid( ) );
		gruCustomer.setAccountGuid( setEmptyValueWhenNullValue(strUserId) );
		gruCustomer.setAccountLogin( "NON RENSEIGNE" );
		gruCustomer.setMobilePhone( setEmptyValueWhenNullValue(user.getTelephoneNumber() ));
		gruCustomer.setExtrasAttributes( "NON RENSEIGNE");
		return gruCustomer;
		
    }
    
    
    private static String setEmptyValueWhenNullValue(String value) 
    {
    	return (value == null) ? "":value;
    }
    
    /**
     * Method which create a demand from Data base, a flux and GRU database
     *
     * @param gruCustomer
     * @return
     */
    private static Customer buildCustomer( fr.paris.lutece.plugins.gru.business.customer.Customer gruCustomer )
    {
        Customer grusupplyCustomer = new Customer(  );
        grusupplyCustomer.setCustomerId( gruCustomer.getId(  ) );
        grusupplyCustomer.setName( gruCustomer.getLastname(  ) );
        grusupplyCustomer.setFirstName( gruCustomer.getFirstname(  ) );
        grusupplyCustomer.setEmail(gruCustomer.getEmail());
        grusupplyCustomer.setTelephoneNumber( gruCustomer.getMobilePhone() );
        
        
        /*        grusupplyCustomer.setBirthday( gruCustomer.getBirthday(  ) );
         grusupplyCustomer.setCivility( gruCustomer.getCivility(  ) );
         grusupplyCustomer.setStreet( gruCustomer.getStreet(  ) );
         grusupplyCustomer.setCityOfBirth( gruCustomer.getCityOfBirth(  ) );
         grusupplyCustomer.setCity( gruCustomer.getCity(  ) );
         grusupplyCustomer.setPostalCode( gruCustomer.getPostalCode(  ) );
         */
        grusupplyCustomer.setEmail( gruCustomer.getEmail(  ) );
        grusupplyCustomer.setStayConnected( true );

        // TODO PROBLEME DE CHAMPS
        
        return grusupplyCustomer;
    }

    /**
     * Method which create a demand from an flux
     * @param notifDTO
     * @param nCustomerId
     * @return
     */
    private static Demand buildDemand( NotificationDTO notifDTO, Customer user )
    {
        Demand demand = new Demand(  );
        demand.setCustomer(user);
        demand.setDemandId( notifDTO.getDemandeId(  ) );
        demand.setDemandIdType( notifDTO.getDemandIdType(  ) );
        demand.setDemandMaxStep( -1 );
        demand.setDemandUserCurrentStep( -1 );
        demand.setDemandState( notifDTO.getDemandState(  ) );
        demand.setNotifType( notifDTO.getNotificationType(  ) );
        demand.setDateDemand( "NON RENSEIGNE" );
        demand.setCRMStatus( notifDTO.getCrmStatusId(  ) );
        demand.setReference( "NON RENSEIGNE" );
        return demand;
    }

    /**
     * Method which create a notification from a flux
     * @param notifDTO
     * @return
     */
    private static Notification buildNotif( NotificationDTO notifDTO, Demand demand, String strJson )
    {
        Notification notification = new Notification(  );
        notification.setDemand(demand);
        notification.setUserEmail( notifDTO.getUserEmail(  ) );
        notification.setUserDashBoard( notifDTO.getUserDashBoard(  ) );
        notification.setUserSms( notifDTO.getUserSms(  ) );
        notification.setUserBackOffice( notifDTO.getUserBackOffice(  ) );
        notification.setJson( strJson );

        return notification;
    }

    /**
     * Build an error response
     * @param strMessage The error message
     * @return The response
     */
    private Response error( String strMessage )
    {
        return error( strMessage, null );
    }

    /**
     * Build an error response
     * @param strMessage The error message
     * @param ex An exception
     * @return The response
     */
    private Response error( String strMessage, Throwable ex )
    {
        if ( ex != null )
        {
            AppLogService.error( strMessage, ex );
        }
        else
        {
            AppLogService.error( strMessage );
        }

        String strError = "{ \"status\": \"Error : " + strMessage + "\" }";

        return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( strError ).build(  );
    }
}
