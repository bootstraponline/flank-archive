package ftl.domain

import ftl.args.IArgs
import ftl.doctor.validateYaml
import ftl.presentation.Output
import ftl.presentation.cli.firebase.test.processValidation
import ftl.run.exception.YmlValidationError
import java.nio.file.Paths

interface RunDoctor : Output {
    val configPath: String
    val fix: Boolean

    data class Error(
        val parsingErrors: List<String> = emptyList(),
        val topLevelUnknownKeys: List<String> = emptyList(),
        val nestedUnknownKeys: Map<String, List<String>> = emptyMap(),
        val invalidDevices: List<InvalidDevice> = emptyList()
    ) {
        companion object {
            val EMPTY = Error()
        }

        data class InvalidDevice(
            val version: String,
            val model: String
        )
    }
}

operator fun RunDoctor.invoke(args: IArgs.ICompanion) {
    val validationResult = validateYaml(args, path = Paths.get(configPath))
    if (fix) processValidation(Paths.get(configPath))
    validationResult.out()
    if (validationResult != RunDoctor.Error.EMPTY) throw YmlValidationError()
}
