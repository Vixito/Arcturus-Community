package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionWagTail extends PetAction {
    public ActionWagTail() {
        super(PetTasks.WAG_TAIL, false);
        this.statusToSet.add(RoomUnitStatus.WAG_TAIL);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.getRoomUnit().setStatus(RoomUnitStatus.WAG_TAIL, "0");
        pet.getRoomUnit().setStatus(RoomUnitStatus.GESTURE, "wav");
        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.WAG_TAIL, null, false), 2500);

        if (pet.getHappyness() > 50) {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        } else {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_HAPPY));
        }

        return true;
    }
}
