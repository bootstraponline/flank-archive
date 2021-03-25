package ftl.cli.firebase.test.ios.configuration

import ftl.config.FtlConstants
import ftl.domain.DescribeIosLocales
import ftl.domain.invoke
import picocli.CommandLine

@CommandLine.Command(
    name = "describe",
    headerHeading = "",
    synopsisHeading = "%n",
    descriptionHeading = "%n@|bold,underline Description:|@%n%n",
    parameterListHeading = "%n@|bold,underline Parameters:|@%n",
    optionListHeading = "%n@|bold,underline Options:|@%n",
    header = ["Describe a locales "],
    usageHelpAutoWidth = true
)
class IosLocalesDescribeCommand :
    Runnable,
    DescribeIosLocales {

    @CommandLine.Parameters(
        index = "0",
        arity = "1",
        paramLabel = "LOCALE",
        defaultValue = "",
        description = [
            "The locale to describe, found" +
                " using \$ gcloud firebase test ios locales list\n."
        ]
    )
    override var locale: String = ""

    @CommandLine.Option(
        names = ["-c", "--config"],
        description = ["YAML config file path"]
    )
    override var configPath: String = FtlConstants.defaultIosConfig

    override fun run() = invoke()
}
