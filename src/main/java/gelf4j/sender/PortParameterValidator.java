package gelf4j.sender;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * Simple validator for port numbers (1-65535)
 */
public class PortParameterValidator implements IParameterValidator {
    public void validate(String name, String value) throws ParameterException {
        int n = Integer.parseInt(value);
        if (n < 1 || n > 65535) {
            throw new ParameterException("Parameter " + name + " should be between 1 and 65535 (found " + value + ")");
        }
    }
}


