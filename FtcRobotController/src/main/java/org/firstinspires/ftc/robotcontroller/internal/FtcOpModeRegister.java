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
import android.os.Environment;
import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegister;

import org.firstinspires.ftc.robotcontroller.external.samples.ConceptNullOp;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta;

import java.lang.reflect.InvocationTargetException;

import dalvik.system.DexClassLoader;

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
    public void register(OpModeManager manager) {
        /**
         * Any manual OpMode class registrations should go here.
         */

        Log.i("DynLoad", "Preparing to load programs");

        final String path = Environment.getExternalStorageDirectory() + "/programs.dex";
        final ClassLoader loader = new DexClassLoader(
            path,
            activity.getDir("outdex", 0).getAbsolutePath(),
            null,
            activity.getClassLoader()
        );

        Log.i("DynLoad", "ClassLoader initialized");

        try {
            final Class<Object> manifest = (Class<Object>)loader.loadClass("com.aercie.manifest.ManifestKt");
            Log.i("DynLoad", "Manifest loaded");

            final Object[] programs = (Object[])manifest.getMethod("getPrograms").invoke(null);
            Log.i("DynLoad", "Manifest retrieved");

            Log.i("DynLoad", "Loading " + programs.length + " programs.");
            for (Object entry : programs) {
                Class<? extends Object> c = entry.getClass();

                String programName = (String)c.getField("program").get(entry);
                String name = (String)c.getField("name").get(entry);
                String group = (String)c.getField("group").get(entry);
                int type = (int)c.getField("type").get(entry);
                Log.i("DynLoad", "Retrieved program entry: " + programName + " (" + name + ")");

                Class<? extends OpMode> program = (Class<? extends OpMode>)loader.loadClass(programName);
                Log.i("DynLoad", "Program loaded");

                if (type == 2)
                    continue;

                OpModeMeta.Flavor flavor =
                    type == 0 ? OpModeMeta.Flavor.AUTONOMOUS :
                    type == 1 ? OpModeMeta.Flavor.TELEOP :
                    null;

                manager.register(new OpModeMeta.Builder()
                    .setFlavor(flavor)
                    .setName(name)
                    .setGroup(group)
                    .build(),
                    program
                );

                Log.i("DynLoad", "Program " + programName + " has been successfully registered");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DynLoad", "DynLoad has encountered an error!");
        }
    }
}
