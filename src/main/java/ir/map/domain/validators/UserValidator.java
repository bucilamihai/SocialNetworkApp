package ir.map.domain.validators;

import ir.map.domain.User;

import java.util.Objects;

public class UserValidator implements Validator<User>{
    @Override
    public void validate(User entity) throws ValidationException {
        String errors = "";
        if(Objects.equals(entity.getFirstName(), ""))
            errors += "First name is empty / ";
        if(Objects.equals(entity.getLastName(), ""))
            errors += "Last name is empty / ";
        if(Objects.equals(entity.getEmail(), ""))
            errors += "Email is empty / ";
        if(Objects.equals(entity.getPassword(), ""))
            errors += "Password is empty";
        if(!errors.isEmpty())
            throw new ValidationException(errors);
    }
}
