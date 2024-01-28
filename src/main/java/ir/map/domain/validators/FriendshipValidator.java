package ir.map.domain.validators;

import ir.map.domain.Friendship;

import java.util.Objects;

public class FriendshipValidator implements Validator<Friendship> {
    @Override
    public void validate(Friendship entity) throws ValidationException {
        String errors = "";
        if(Objects.equals(entity.getId().getRight(), entity.getId().getLeft()))
            errors += "Friendship between same user";
        if(!errors.isEmpty())
            throw new ValidationException(errors);
    }
}
