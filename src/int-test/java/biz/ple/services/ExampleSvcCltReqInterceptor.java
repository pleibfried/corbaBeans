package biz.ple.services;

import java.nio.ByteBuffer;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.ple.corba.annotations.CorbaClientInterceptor;
import biz.ple.corba.util.ServiceContextPropagatingClientRequestInterceptorBase;


@CorbaClientInterceptor(serviceName = ExampleService.SERVICE_NAME)
public class ExampleSvcCltReqInterceptor extends ServiceContextPropagatingClientRequestInterceptorBase {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ExampleSvcCltReqInterceptor.class);


    @Override
    public void send_request(ClientRequestInfo ri)
        throws ForwardRequest
    {
        try {
            Any data = piCurrent.get_slot(piCurrentSlotId);
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.asIntBuffer().put(data.extract_long());
            ri.add_request_service_context(new ServiceContext(ExampleService.SERVICE_CONTEXT_ID, buffer.array()), true);
        }
        catch (InvalidSlot ivs) {
            LOG.error("Problem getting info from PI Current.", ivs);
        }
        catch (BAD_OPERATION bop) {
            // do nothing, there's no context data
        }
    }


    @Override
    public void send_poll(ClientRequestInfo ri)
    {
    }


    @Override
    public void receive_reply(ClientRequestInfo ri)
    {
        try {
            ServiceContext ctx = ri.get_reply_service_context(ExampleService.SERVICE_CONTEXT_ID);
            if (ctx.context_data != null && ctx.context_data.length > 0) {
                if (ctx.context_data.length != 4) {
                    LOG.error("Context data has unexpected length " +  ctx.context_data.length + " (should be 4).");
                    return;
                }
                ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.put(ctx.context_data);
                buffer.position(0);
                Any data = orbBean.createAny();
                data.insert_long(buffer.asIntBuffer().get());
                piCurrent.set_slot(piCurrentSlotId, data);
            }
        }
        catch (InvalidSlot argh) {
            LOG.error("Problem setting data on PI Current.", argh);
        }
        catch (BAD_PARAM fail) {
            // Do nothing, no service context data for ExampleService.SERVICE_CONTEXT_ID
        }
    }


    @Override
    public void receive_exception(ClientRequestInfo ri)
        throws ForwardRequest
    {
    }


    @Override
    public void receive_other(ClientRequestInfo ri)
        throws ForwardRequest
    {
    }


    @Override
    public String name()
    {
        return "ExampleServiceClientRequestInterceptor";
    }


    @Override
    public void destroy()
    {
    }

}
