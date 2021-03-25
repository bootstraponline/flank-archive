package ftl.cli.firebase.test.ios.versions

import ftl.config.FtlConstants
import ftl.domain.DescribeIosVersions
import ftl.domain.invoke
import picocli.CommandLine

@CommandLine.Command(
    name = "describe",
    headerHeading = "",
    synopsisHeading = "%n",
    descriptionHeading = "%n@|bold,underline Description:|@%n%n",
    parameterListHeading = "%n@|bold,underline Parameters:|@%n",
    optionListHeading = "%n@|bold,underline Options:|@%n",
    header = ["List of OS versions available to test against"],
    description = ["Print current list of iOS versions available to test against"],
    usageHelpAutoWidth = true
)
class IosVersionsDescribeCommand :
    Runnable,
    DescribeIosVersions {

    @CommandLine.Option(
        names = ["-c", "--config"],
        description = ["YAML config file path"]
    )
    override var configPath: String = FtlConstants.defaultIosConfig

    @CommandLine.Parameters(
        index = "0",
        arity = "1",
        paramLabel = "VERSION_ID",
        defaultValue = "",
        description = [
            "The version to describe, found" +
                " using \$ gcloud firebase test ios versions list."
        ]
    )
    override var versionId: String = ""

    override fun run() = invoke()
}
