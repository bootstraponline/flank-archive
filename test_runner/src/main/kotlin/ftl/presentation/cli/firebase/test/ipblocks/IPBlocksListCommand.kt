package ftl.presentation.cli.firebase.test.ipblocks

import ftl.api.IpBlockList
import ftl.domain.ListIPBlocks
import ftl.domain.invoke
import ftl.presentation.outputLogger
import ftl.presentation.throwUnknownType
import ftl.util.PrintHelpCommand
import picocli.CommandLine

@CommandLine.Command(
    name = "list",
    sortOptions = false,
    headerHeading = "",
    synopsisHeading = "%n",
    descriptionHeading = "%n@|bold,underline Description:|@%n%n",
    parameterListHeading = "%n@|bold,underline Parameters:|@%n",
    optionListHeading = "%n@|bold,underline Options:|@%n",
    header = ["List all IP address blocks used by Firebase Test Lab devices"],
    usageHelpAutoWidth = true
)
class IPBlocksListCommand :
    PrintHelpCommand(),
    ListIPBlocks {

    override fun run() = invoke()

    override val out = outputLogger {
        when (this) {
            is IpBlockList -> toCliTable()
            else -> throwUnknownType()
        }
    }
}
