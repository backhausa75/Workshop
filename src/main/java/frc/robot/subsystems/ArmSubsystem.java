package frc.robot.subsystems;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
//import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.FeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.StaticFeedforwardSignValue;
import com.ctre.phoenix6.sim.CANcoderSimState;
import com.ctre.phoenix6.sim.TalonFXSimState;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import com.ctre.phoenix6.Utils;
import static edu.wpi.first.units.Units.*;
import frc.robot.subsystems.ArmConstants.CANIDS;

public class ArmSubsystem extends SubsystemBase {

    private final TalonFX Arm;
   // private final CANcoder encoder;
    private final CANBus Canivore = new CANBus("CANivore");

    private final MotionMagicVoltage positionOut = new MotionMagicVoltage(0);

    // Simulation components
    private TalonFXSimState armSimState;
    private CANcoderSimState encoderSimState;
    
    // Arm simulation constants and state
    private static final double GEAR_RATIO = 10.0; // Adjust based on your arm gearbox
    private static final double SIM_TIME_STEP = 0.020; // 20ms FRC loop time
    private static final double MOTOR_INERTIA = 0.001; // kg⋅m² moment of inertia
    
    // Simulation state variables
    private double simPosition = 0.0; // Simulated position in rotations
    private double simVelocity = 0.0; // Simulated velocity in rotations per second

    public ArmSubsystem () {

        Arm = new TalonFX(CANIDS.kArmotorId, Canivore);
       // encoder = new CANcoder(CANIDS.kCANcoderId, Canivore);

        TalonFXConfiguration config = new TalonFXConfiguration();

        config.MotorOutput.NeutralMode = NeutralModeValue.Coast;

        config.Slot0.GravityType = GravityTypeValue.Arm_Cosine; // read in cosine
        config.Slot0.kG = ArmConstants.kGArm.get();
        config.Slot0.kS = ArmConstants.kSArm.get();
        config.Slot0.kP = ArmConstants.kpArm.get();
        config.Slot0.kD = ArmConstants.kDArm.get();
        config.Slot0.StaticFeedforwardSign = StaticFeedforwardSignValue.UseClosedLoopSign;
        
        config.MotionMagic.MotionMagicAcceleration = 0;
        config.MotionMagic.MotionMagicCruiseVelocity = 0;
        
        config.Feedback.FeedbackSensorSource = FeedbackSensorSourceValue.RemoteCANcoder;
        //config.Feedback.FeedbackRemoteSensorID = encoder.getDeviceID();

            for (int i = 0; i < 2; ++i) {
      var status = Arm.getConfigurator().apply(config);
      if (status.isOK()) break;
    }

        // Initialize simulation if running in simulation
        if (Utils.isSimulation()) {
            simulationInit();
        }
    }

    public void periodic() {
    }

    /**
     * Initialize simulation physics and motor models.
     * This should be called once during subsystem construction.
     */
    private void simulationInit() {
        // Get sim states from devices
        armSimState = Arm.getSimState();
        //encoderSimState = encoder.getSimState();

        // Configure TalonFX simulator for KrakenX60 motor
        armSimState.setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    /**
     * Update simulation state based on physics calculations.
     * This should be called periodically during simulation to update motor positions and velocities.
     */
    public void simulationPeriodic() {
        if (!Utils.isSimulation()) {
            return;
        }

        // Set the supply voltage from the robot battery
        armSimState.setSupplyVoltage(RobotController.getBatteryVoltage());

        // Get the motor voltage output from the TalonFX controller
        var motorVoltage = armSimState.getMotorVoltageMeasure();

        // Simple physics integration for motor velocity
        // Acceleration = (Voltage * Motor_Constant - Friction) / (Inertia * Gear_Ratio)
        // For simplification, we model: dV/dt = (voltage - friction) / inertia
        double motorVoltageValue = motorVoltage.in(Volts);
        double friction = 0.05; // Simple friction model
        double acceleration = (motorVoltageValue - friction) / MOTOR_INERTIA;
        
        // Update velocity using simple euler integration
        simVelocity += acceleration * SIM_TIME_STEP;
        
        // Apply damping/friction
        simVelocity *= 0.99;
        
        // Update position
        simPosition += simVelocity * SIM_TIME_STEP / GEAR_RATIO;

        // Set the rotor position and velocity in the TalonFX simulator
        // Note: rotor position is mechanism position * gear ratio
        armSimState.setRawRotorPosition(simPosition * GEAR_RATIO);
        armSimState.setRotorVelocity(simVelocity);

        // Update the CANcoder to match the arm position
        encoderSimState.setRawPosition(simPosition);
        encoderSimState.setVelocity(simVelocity / GEAR_RATIO);
    }

    public void stopMotor () {
        Arm.stopMotor();
    }

        public void setPosition(double position) {
        // Apply the position output to the leader motor
        Arm.setControl(positionOut.withPosition(position));
    }

    //stop arm motor
    public Command stopCommand() {
        return runOnce(() -> stopMotor());
  }
    // Run motor to horizontal position
    public Command horizontal () {
        return runOnce(() -> setPosition(0.25));
  }
    // Run motor to vertical position
    public Command vertical () {
        return runOnce(() -> setPosition(0));
    }
}