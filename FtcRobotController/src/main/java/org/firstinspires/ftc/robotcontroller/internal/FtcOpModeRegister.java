/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package org.firstinspires.ftc.robotcontroller.internal;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Environment;
import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegister;

import org.firstinspires.ftc.robotcontroller.external.samples.ConceptNullOp;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import dalvik.system.DexClassLoader;
import io.arct.ftc.eventloop.OperationMode.Type;

/**
 * {@link FtcOpModeRegister} is responsible for registering opmodes for use in an FTC game.
 * @see #register(OpModeManager)
 */
public class FtcOpModeRegister implements OpModeRegister {
    private final Activity activity;

    public FtcOpModeRegister(Activity activity) {
        this.activity = activity;
    }

    /**
     * {@link #register(OpModeManager)} is called by the SDK game in order to register
     * OpMode classes or instances that will participate in an FTC game.
     *
     * There are two mechanisms by which an OpMode may be registered.
     *
     *  1) The preferred method is by means of class annotations in the OpMode itself.
     *  See, for example the class annotations in {@link ConceptNullOp}.
     *
     *  2) The other, retired,  method is to modify this {@link #register(OpModeManager)}
     *  method to include explicit calls to OpModeManager.register().
     *  This method of modifying this file directly is discouraged, as it
     *  makes updates to the SDK harder to integrate into your code.
     *
     * @param manager the object which contains methods for carrying out OpMode registrations
     *
     * @see com.qualcomm.robotcore.eventloop.opmode.TeleOp
     * @see com.qualcomm.robotcore.eventloop.opmode.Autonomous
     */
    @SuppressWarnings("unchecked")
    public void register(OpModeManager manager) {
        /**
         * Any manual OpMode class registrations should go here.
         */

        try {
            final String path = Environment.getExternalStorageDirectory() + "/programs.dex";

            final ClassLoader loader = new DexClassLoader(
                path,
                activity.getDir("outdex", 0).getAbsolutePath(),
                null,
                activity.getClassLoader()
            );

            final Class<Object> manifest = (Class<Object>) loader.loadClass("io.arct.ftc.manifest.ProgramManifest");
            final List<String> programs = (List<String>) manifest.getMethod("programs").invoke(manifest.newInstance());

            for (String name : programs) {
                Class<? extends OpMode> program = (Class<? extends OpMode>) loader.loadClass(name);
                Object instance = program.newInstance();

                String registeredName = (String) program.getMethod("getName").invoke(instance);
                String registeredGroup = (String) program.getMethod("getGroup").invoke(instance);
                Type registeredType = (Type) program.getMethod("getType").invoke(instance);

                OpModeMeta.Flavor flavor;

                if (registeredType == Type.Autonomous)
                    flavor = OpModeMeta.Flavor.AUTONOMOUS;
                else if (registeredType == Type.Operated)
                    flavor = OpModeMeta.Flavor.TELEOP;
                else continue;

                manager.register(new OpModeMeta.Builder()
                    .setFlavor(flavor)
                    .setName(registeredName)
                    .setGroup(registeredGroup)
                    .build(),
                    program
                );
            }

        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
