package biz.ple.corba.util;

import java.math.BigDecimal;

import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.PortableInterceptor.InvalidSlot;

import biz.ple.corba.interfaces.PICurrentInjectable;;


/**
 * <p>Convenience base class for a simple Service Context Manager.</p>
 * <p>The essential (and often only) functionality of a Service Context Manager is
 * to associate some bit of application data to the current thread by assigning it
 * to a specific 'slot' of the PICurrent.</p>
 * <p>The PICurrent can be thought of as a sophisticated variant of the
 * {@link ThreadLocal} which ensures that data associated with a thread gets
 * transparently passed on to another thread, e.g. if request receipt and request
 * processing occur in different threads (which is the case in many CORBA
 * implementations). That ensures that the information in a slot can reliably be
 * picked up by Request Interceptors as if application code and interceptor code
 * were executed in the same thread, even when that is not the case.</p>
 * <p>This class implements the {@link PICurrentInjectable} interface and provides
 * a set of protected methods which simplify getting and setting the slot data for
 * simple data types. It does not have any public methods. Application specific
 * public methods must be provided in a derived, concrete class.
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 */
public abstract class ServiceContextManagerBase extends PICurrentInjectableImpl {

    public ServiceContextManagerBase() {
        piCurrentSlotId = -1;
    }

    protected class AnyBuilder {

        private Any anyVal;

        public AnyBuilder()
        {
            anyVal = orbBean.createAny();
        }

        public AnyBuilder withShortValue(short val)
        {
            anyVal.insert_short(val);
            return this;
        }

        public AnyBuilder withLongValue(int val)
        {
            anyVal.insert_long(val);
            return this;
        }

        public AnyBuilder withLongLongValue(long val)
        {
            anyVal.insert_longlong(val);
            return this;
        }

        public AnyBuilder withFloatValue(float val)
        {
            anyVal.insert_float(val);
            return this;
        }

        public AnyBuilder withDoubleValue(double val)
        {
            anyVal.insert_double(val);
            return this;
        }

        public AnyBuilder withFixedValue(BigDecimal val)
        {
            anyVal.insert_fixed(val);
            return this;
        }

        public AnyBuilder withStringValue(String val)
        {
            anyVal.insert_wstring(val);
            return this;
        }

        public AnyBuilder withBoolValue(boolean val)
        {
            anyVal.insert_boolean(val);
            return this;
        }

        public Any value()
        {
            return anyVal;
        }

    }

    protected void setSlotContents(AnyBuilder anyBld)
    {
        try {
            piCurrent.set_slot(piCurrentSlotId, anyBld.value());
        }
        catch (InvalidSlot isl) {
            throw new RuntimeException("Failed to set PICurrent slot with id " + piCurrentSlotId, isl);
        }
    }


    protected Any getSlotContents()
    {
        try {
            return piCurrent.get_slot(piCurrentSlotId);
        }
        catch (InvalidSlot isl) {
            throw new RuntimeException("Failed to get data from PICurrent slot with id " + piCurrentSlotId, isl);
        }
    }


    protected void setShortData(short corbaShort)
    {
        setSlotContents(new AnyBuilder().withShortValue(corbaShort));
    }


    protected void setLongData(int corbaLong)
    {
        setSlotContents(new AnyBuilder().withLongValue(corbaLong));
    }


    protected void setLongLongData(long corbaLongLong)
    {
        setSlotContents(new AnyBuilder().withLongLongValue(corbaLongLong));
    }


    protected void setFloatData(float corbaFloat)
    {
        setSlotContents(new AnyBuilder().withFloatValue(corbaFloat));
    }


    protected void setDoubleData(double corbaDouble)
    {
        setSlotContents(new AnyBuilder().withDoubleValue(corbaDouble));
    }


    protected void setFixedData(BigDecimal corbaFixed)
    {
        setSlotContents(new AnyBuilder().withFixedValue(corbaFixed));
    }


    protected void setStringData(String corbaWString)
    {
        setSlotContents(new AnyBuilder().withStringValue(corbaWString));
    }


    protected void setBooleanData(boolean corbaBool)
    {
        setSlotContents(new AnyBuilder().withBoolValue(corbaBool));
    }


    /**
     * Sets the slot data to 'null', i.e. an empty Any.
     */
    protected void setNullData()
    {
        setSlotContents(new AnyBuilder());
    }


    protected Boolean getBooleanData()
    {
        Any data = getSlotContents();
        if (data != null && data.type().kind().equals(TCKind.tk_boolean)) {
            return data.extract_boolean();
        }
        return null;
    }


    protected Short getShortData()
    {
        Any data = getSlotContents();
        if (data != null && data.type().kind().equals(TCKind.tk_short)) {
            return data.extract_short();
        }
        return null;
    }


    protected Integer getLongData()
    {
        Any data = getSlotContents();
        if (data != null && data.type().kind().equals(TCKind.tk_long)) {
            return data.extract_long();
        }
        return null;
    }


    protected Long  getLongLongData()
    {
        Any data = getSlotContents();
        if (data != null && data.type().kind().equals(TCKind.tk_longlong)) {
            return data.extract_longlong();
        }
        return null;
    }


    protected Float getFloatData()
    {
        Any data = getSlotContents();
        if (data != null && data.type().kind().equals(TCKind.tk_float)) {
            return data.extract_float();
        }
        return null;
    }


    protected Double getDoubleData()
    {
        Any data = getSlotContents();
        if (data != null && data.type().kind().equals(TCKind.tk_double)) {
            return data.extract_double();
        }
        return null;
    }


    protected BigDecimal getFixedData()
    {
        Any data = getSlotContents();
        if (data != null && data.type().kind().equals(TCKind.tk_fixed)) {
            return data.extract_fixed();
        }
        return null;
    }


    protected String getStringData()
    {
        Any data = getSlotContents();
        if (data != null && data.type().kind().equals(TCKind.tk_string)) {
            return data.extract_string();
        }
        return null;
    }

}
