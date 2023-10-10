import org.apache.tools.ant.taskdefs.condition.Os

if (Os.isFamily(Os.FAMILY_UNIX) && !Os.isFamily(Os.FAMILY_MAC)) {
  include("nosession_libc")
}
if (Os.isFamily(Os.FAMILY_WINDOWS)) {
  include("sandbox")
}
