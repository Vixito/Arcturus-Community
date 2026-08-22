package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionDance extends PetAction {
    public ActionDance(PetTasks task) {
        super(task, false);
        this.statusToSet.add(RoomUnitStatus.DANCE);
    }

    public ActionDance() {
        this(PetTasks.DANCE);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.getRoomUnit().setStatus(RoomUnitStatus.DANCE, "0");
        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.DANCE, null, false), 3000);

        if (pet.getHappyness() > 50) {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        } else {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_HAPPY));
        }

        return true;
    }
}
