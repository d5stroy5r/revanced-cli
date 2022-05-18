package app.revanced.cli

import app.revanced.patch.Patches
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.findAnnotationRecursively
import app.revanced.utils.adb.Adb
import picocli.CommandLine.*
import java.io.File

@Command(
    name = "ReVanced-CLI", version = ["1.0.0"], mixinStandardHelpOptions = true
)
internal object MainCommand : Runnable {
    @Parameters(
        paramLabel = "INCLUDE",
        description = ["Which patches to include. If none is specified, all compatible patches will be included"]
    )
    internal var includedPatches = arrayOf<String>()

    @Option(names = ["-p", "--patches"], description = ["One or more bundles of patches"])
    internal var patchBundles = arrayOf<File>()

    @Option(names = ["-t", "--temp-dir"], description = ["Temporal resource cache directory"], required = true)
    internal lateinit var cacheDirectory: String

    @Option(names = ["-r", "--resource-patcher"], description = ["Enable patching resources"])
    internal var patchResources: Boolean = false

    @Option(
        names = ["-c", "--clean"],
        description = ["Clean the temporal resource cache directory. This will be done anyways when running the patcher"]
    )
    internal var clean: Boolean = false

    @Option(names = ["-l", "--list"], description = ["List patches only"])
    internal var listOnly: Boolean = false

    @Option(names = ["-m", "--merge"], description = ["One or more dex file containers to merge"])
    internal var mergeFiles = listOf<File>()

    @Option(names = ["-a", "--apk"], description = ["Input file to be patched"], required = true)
    internal lateinit var inputFile: File

    @Option(names = ["-o", "--out"], description = ["Output file path"], required = true)
    internal lateinit var outputPath: String

    @Option(names = ["-d", "--deploy-on"], description = ["If specified, deploy to adb device with given name"])
    internal var deploy: String? = null

    override fun run() {
        if (listOnly) {
            for (patchBundle in patchBundles) for (it in Patches.load(patchBundle)) println(
                "[available] ${
                    it.javaClass.findAnnotationRecursively(
                        Name::class.java
                    )?.name ?: Name::class.java.name
                }"
            )
            return
        }

        val outputFile = File(outputPath)

        val patcher = app.revanced.patcher.Patcher(
            inputFile, cacheDirectory, patchResources
        )

        var adb: Adb? = null
        deploy?.let {
            adb = Adb(
                outputFile, patcher.packageName, deploy!!
            )
        }

        Patcher.start(patcher)

        if (clean) {
            File(cacheDirectory).deleteRecursively()
            outputFile.delete()
        }

        adb?.deploy()
    }
}