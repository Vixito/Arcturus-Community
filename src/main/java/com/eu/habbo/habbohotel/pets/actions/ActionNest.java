package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionNest extends PetAction {
    public ActionNest() {
        super(PetTasks.NEST, true);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();
        pet.findNest();

        if (pet.getEnergy() < 30) {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.TIRED));
        } else {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.SLEEPING));
        }

        return true;
    }
}
