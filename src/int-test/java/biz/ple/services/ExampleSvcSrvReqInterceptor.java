package biz.ple.services;

import java.nio.ByteBuffer;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.ple.corba.annotations.CorbaServerInterceptor;
import biz.ple.corba.util.ServiceContextPropagatingServerRequestInterceptorBase;


@CorbaServerInterceptor(serviceName = ExampleService.SERVICE_NAME)
public class ExampleSvcSrvReqInterceptor extends ServiceContextPropagatingServerRequestInterceptorBase {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ExampleSvcSrvReqInterceptor.class);


    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri)
        throws ForwardRequest
    {
    }


    @Override
    public void receive_request(ServerRequestInfo ri)
        throws ForwardRequest
    {
        try {
            ServiceContext ctx = ri.get_request_service_context(ExampleService.SERVICE_CONTEXT_ID);
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
            // Do nothing, there's simply no context data
        }
    }


    @Override
    public void send_reply(ServerRequestInfo ri)
    {
        try {
            Any data = piCurrent.get_slot(piCurrentSlotId);
            int val = data.extract_long();
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.asIntBuffer().put(val + 1);
            ri.add_reply_service_context(new ServiceContext(ExampleService.SERVICE_CONTEXT_ID, buffer.array()), true);
        }
        catch (InvalidSlot ivs) {
            LOG.error("Problem getting info from PI Current.", ivs);
            System.err.println("Invalid Slot ID in send_reply");
        }
        catch (BAD_OPERATION bop) {
            // do nothing, there's no context data
        }
    }


    @Override
    public void send_exception(ServerRequestInfo ri)
        throws ForwardRequest
    {
    }


    @Override
    public void send_other(ServerRequestInfo ri)
        throws ForwardRequest
    {
    }


    @Override
    public String name()
    {
        return "ExampleServiceServerRequestInterceptor";
    }


    @Override
    public void destroy()
    {
    }

}
