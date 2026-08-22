package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionPlay extends PetAction {
    public ActionPlay() {
        super(PetTasks.PLAY, true);
        this.statusToSet.add(RoomUnitStatus.PLAY);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();
        pet.findToy();
        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.PLAY, null, false), 3000);

        if (pet.getHappyness() > 50) {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        } else {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_HAPPY));
        }

        return true;
    }
}
