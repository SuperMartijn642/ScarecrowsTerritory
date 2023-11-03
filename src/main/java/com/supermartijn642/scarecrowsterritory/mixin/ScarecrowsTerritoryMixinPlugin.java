package com.supermartijn642.scarecrowsterritory.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class ScarecrowsTerritoryMixinPlugin implements IMixinConfigPlugin {

    private boolean isInControlLoaded;

    @Override
    public void onLoad(String mixinPackage){
        this.isInControlLoaded = isClassAvailable("mcjty.incontrol.InControl");
    }

    private static boolean isClassAvailable(String location){
        try{
            MixinService.getService().getBytecodeProvider().getClassNode(location);
            return true;
        }catch(Exception ignored){
            return false;
        }
    }

    @Override
    public String getRefMapperConfig(){
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        return this.isInControlLoaded || !mixinClassName.startsWith("in_control.");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){
    }

    @Override
    public List<String> getMixins(){
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){
    }
}
