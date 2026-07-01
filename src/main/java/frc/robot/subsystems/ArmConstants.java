package frc.robot.subsystems;

import frc.robot.util.LoggedTunableNumber;

public class ArmConstants {

        public final class CANIDS {
                public static final int kArmotorId = 16;
                //public static final int kCANcoderId = 34;
        }

        public static final LoggedTunableNumber kpArm = new LoggedTunableNumber("Arm/kp", 0);
        public static final LoggedTunableNumber kIArm = new LoggedTunableNumber("Arm/kI", 0);
        public static final LoggedTunableNumber kDArm = new LoggedTunableNumber("Arm/kD", 0);
        public static final LoggedTunableNumber kGArm = new LoggedTunableNumber("Arm/kG", 0);
        public static final LoggedTunableNumber kSArm = new LoggedTunableNumber("Arm/kS", 0);
}
