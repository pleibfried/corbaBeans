package biz.ple.corba.beans.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Represents a set of POA Policies. To set the Policies of a POA created by instantiating a
 * {@link PoaBean}, an instance of {@code PoaPolicies} must be passed to the {@code PoaBean}'s
 * constructor.
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 * @see    PoaBean#PoaBean(PoaBean, String, PoaPolicies)
 *
 */
public class PoaPolicies {

    Map<PoaPolicyName, PoaPolicyValue> contents;

    /**
     * Default constructor; all of the POA Policies in the newly created object will be
     * set to default values, as follows:
     * <ul>
     * <li>IdAssignment - System</li>
     * <li>IdUniqueness - Unique</li>
     * <li>ImplicitActivation - NoImplicitActivation</li>
     * <li>Lifespan - Transient</li>
     * <li>RequestProcessing - UseActiveObjectMapOnly</li>
     * <li>ServantRetention - Retain</li>
     * <li>Thread - OrbCtrlModel</li>
     * </ul>
     */
    public PoaPolicies()
    {
        contents = new HashMap<PoaPolicyName, PoaPolicyValue>();
        contents.put(PoaPolicyName.IdAssignment, PoaPolicyValue.System);
        contents.put(PoaPolicyName.IdUniqueness, PoaPolicyValue.Unique);
        contents.put(PoaPolicyName.ImplicitActivation, PoaPolicyValue.NoImplicitActivation);
        contents.put(PoaPolicyName.Lifespan, PoaPolicyValue.Transient);
        contents.put(PoaPolicyName.RequestProcessing, PoaPolicyValue.UseActiveObjectMapOnly);
        contents.put(PoaPolicyName.ServantRetention, PoaPolicyValue.Retain);
        contents.put(PoaPolicyName.Thread, PoaPolicyValue.OrbCtrlModel);
    }


    /**
     * Constructor: creates a new PoaPolicies object as a copy from an existing one
     * (i.e. with the same value for each POA Policy).
     * @param other
     *      The PoaPolicies object whose values should be copied.
     */
    public PoaPolicies(PoaPolicies other)
    {
        contents = new HashMap<PoaPolicyName, PoaPolicyValue>(other.contents);
    }


    /**
     * Sets a policy to a certain value, overwriting any previous value.
     * @param name
     *      The name of the POA Policy to set.
     * @param value
     *      The new value of the POA Policy identified by {@code name}.
     * @throws IllegalArgumentException
     *      if name and value do not match, e.g. when the name is {@code ServantRetention} and
     *      the value is {@code System}.
     */
    public void setPolicy(PoaPolicyName name, PoaPolicyValue value)
    {
        if (name == null) {
            throw new IllegalArgumentException("POA policy name may not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("POA policy value may not be null.");
        }

        String fieldName = value.toMemberName();
        Class<?> omgClass = name.toOmgClass();
        try {
            Integer omgValue = omgClass.getField(fieldName).getInt(null);
            if (omgValue.intValue() != value.intValue()) {
            	throw new IllegalStateException("There's something wrong with the POA Policy wrapper classes.");
            }
            contents.put(name, value);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("Surprisingly, the field '" + omgClass.getSimpleName() + '.' +
                                            fieldName + "' does not seem to be public.");
        }
        catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("'" + value + "' is not a legal value for policy '" + name + "'.");
        }
    }


    /**
     * Sets multiple POA Policies at once.
     * @param policies
     *      A map containing the POA Policies to set. The effect of the method is the
     *      same as if calling {@link #setPolicy(PoaPolicyName, PoaPolicyValue) setPolicy()}
     *      for each key/value-pair in the map.
     */
    public void setPolicies(Map<PoaPolicyName, PoaPolicyValue> policies)
    {
        for (Entry<PoaPolicyName, PoaPolicyValue> entry: policies.entrySet()) {
            setPolicy(entry.getKey(), entry.getValue());
        }
    }


    /**
     * Reads the value of a specific POA Policy.
     * @param name
     *      The name of a POA Policy.
     * @return
     *      The value of the POA Policy identified by {@code name}.
     */
    public PoaPolicyValue getPolicyValue(PoaPolicyName name)
    {
        return contents.get(name);
    }


    /**
     * Reads all POA Policies in this {@code PoaPolicies} object.
     * @return
     *      A read-only set of all key/value pairs making up the POA Policy set
     *      represented by this {@code PoaPolicies} object.
     */
    public Set<Entry<PoaPolicyName, PoaPolicyValue>> entrySet()
    {
        return Collections.unmodifiableSet(contents.entrySet());
    }


    /**
     * Reads the number of POA Policies in this {@code PoaPolicies} object.
     * @return
     *      The number of POA Policies in the POA Policy set represented by this
     *      {@code PoaPolicies} object. Since all available POA Policies are set
     *      by the constructor and there is no way of deleting a Policy, this
     *      method always returns 7.
     */
    public int size()
    {
        return contents.size();
    }

}
