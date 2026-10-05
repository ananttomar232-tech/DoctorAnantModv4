package com.dranant.network;

import com.dranant.DoctorAnantMod;
import com.dranant.entity.BikeEntity;
import com.dranant.entity.SupercarEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: a vehicle key press from the driver (horn, lights, doors, bonnet, boot, nitro on/off). */
public record CarControlPayload(int action) implements CustomPacketPayload {
    public static final Type<CarControlPayload> TYPE = new Type<>(DoctorAnantMod.id("car_control"));
    public static final StreamCodec<ByteBuf, CarControlPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CarControlPayload::action, CarControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(int action) {
        PacketDistributor.sendToServer(new CarControlPayload(action));
    }

    public static void handle(CarControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.getVehicle() instanceof SupercarEntity car) {
                car.handleControl(payload.action(), player);
            } else if (player.getVehicle() instanceof BikeEntity bike && payload.action() == SupercarEntity.CONTROL_HORN) {
                bike.honk();
            }
        });
    }
}
