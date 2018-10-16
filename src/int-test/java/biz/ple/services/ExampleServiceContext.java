package biz.ple.services;

import biz.ple.corba.annotations.CorbaServiceContextManager;
import biz.ple.corba.util.ServiceContextManagerBase;


@CorbaServiceContextManager(serviceName = ExampleService.SERVICE_NAME)
public class ExampleServiceContext extends ServiceContextManagerBase {

    public void setData(Integer number)
    {
        if (number != null) {
            setLongData(number);
        }
        else {
            setNullData();
        }
    }


    public Integer getData()
    {
        return getLongData();
    }

}
