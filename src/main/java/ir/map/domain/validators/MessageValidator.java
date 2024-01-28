package ir.map.domain.validators;

import ir.map.domain.Message;

import java.util.Objects;

public class MessageValidator implements Validator<Message>{
    @Override
    public void validate(Message entity) throws ValidationException {
        String errors = "";
        if(Objects.equals(entity.getMessage(), ""))
            errors += "Message is empty";
        if(!errors.isEmpty())
            throw new ValidationException(errors);
    }
}
