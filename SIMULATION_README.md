# ArmSubsystem Simulation Implementation

## Overview
The `ArmSubsystem` now includes full TalonFX and WPILib simulation support, allowing you to test arm movements in simulation without hardware.

## What Was Added

### 1. Simulation Initialization (`simulationInit()`)
- Called automatically during construction when `Utils.isSimulation()` is true
- Retrieves simulation states from the TalonFX motor and CANcoder encoder
- Configures the TalonFX simulator for KrakenX60 motor specifications

### 2. Simulation Physics (`simulationPeriodic()`)
- Called from `Robot.simulationPeriodic()` to update simulation each loop
- Implements a physics-based model with:
  - **Supply Voltage**: Uses `RobotController.getBatteryVoltage()` to simulate realistic battery behavior
  - **Motor Model**: Simple Euler integration physics model based on voltage input
  - **Friction**: Includes basic friction (5V threshold) and damping (99% velocity retention)
  - **Inertia**: Configurable moment of inertia (0.001 kg⋅m²)
  - **Gear Ratio**: Configurable gear ratio (default 10:1)

### 3. Simulation State Variables
```java
private double simPosition = 0.0;    // Current position in rotations
private double simVelocity = 0.0;    // Current velocity in rotations per second
```

### 4. Physics Constants
```java
private static final double GEAR_RATIO = 10.0;        // Adjust to your arm gearbox
private static final double SIM_TIME_STEP = 0.020;    // 20ms FRC loop time
private static final double MOTOR_INERTIA = 0.001;    // kg⋅m² moment of inertia
```

## How It Works

### Simulation Loop
1. **Get Motor Voltage**: Read the voltage command from the TalonFX controller
   ```java
   var motorVoltage = armSimState.getMotorVoltageMeasure();
   ```

2. **Calculate Acceleration**: Based on voltage, friction, and inertia
   ```java
   double acceleration = (motorVoltageValue - friction) / MOTOR_INERTIA;
   ```

3. **Update Velocity**: Use Euler integration with damping
   ```java
   simVelocity += acceleration * SIM_TIME_STEP;  // Integrate acceleration
   simVelocity *= 0.99;                          // Apply damping
   ```

4. **Update Position**: Integrate velocity, accounting for gear ratio
   ```java
   simPosition += simVelocity * SIM_TIME_STEP / GEAR_RATIO;
   ```

5. **Update SimState**: Set the simulated motor and encoder positions
   ```java
   armSimState.setRawRotorPosition(simPosition * GEAR_RATIO);
   armSimState.setRotorVelocity(simVelocity);
   encoderSimState.setRawPosition(simPosition);
   encoderSimState.setVelocity(simVelocity / GEAR_RATIO);
   ```

## Key Design Decisions

### Rotor vs Mechanism Position
- **Mechanism Position**: Position after the gearbox (simPosition)
- **Rotor Position**: Position before the gearbox (simPosition × GEAR_RATIO)
- The TalonFX stores rotor position, while the CANcoder stores mechanism position

### Physics Model
The implementation uses a simplified physics model suitable for FRC:
- **Accurate enough** for testing control logic and commands
- **Simple enough** to run without external libraries
- **Real-world behavior** includes friction, damping, and inertia effects

### Automatic Simulation Detection
Uses `Utils.isSimulation()` to automatically enable simulation code:
- In simulation: Physics model is active, position updates based on voltage
- On hardware: Physics model is disabled, motor reports real sensor data

## Integration Points

### RobotContainer
```java
private final ArmSubsystem armSubsystem = new ArmSubsystem();

public ArmSubsystem getArmSubsystem() {
    return armSubsystem;
}
```

### Robot.java
```java
@Override
public void simulationPeriodic() {
    // ... existing code ...
    m_robotContainer.getArmSubsystem().simulationPeriodic();
}
```

## Testing Simulation

### In VS Code
1. **Run Simulation**: Select "Simulate" in the VS Code WPILib menu
2. **Test Commands**: Use the dashboard or test your arm commands
3. **Monitor Position**: The simulated position will update based on commands

### Physics Parameters to Tune
Adjust these constants based on your arm's actual behavior:

```java
GEAR_RATIO = 10.0;              // Your actual gear ratio
MOTOR_INERTIA = 0.001;          // Affect how quickly it accelerates/decelerates
friction = 0.05;                // Minimum voltage to overcome friction
damping (0.99);                 // How much velocity is lost each cycle
```

## Documentation References

- **Phoenix 6 Simulation**: https://v6.docs.ctr-electronics.com/en/stable/docs/api-reference/simulation/simulation-intro.html
- **TalonFX SimState API**: https://api.ctr-electronics.com/phoenix6/stable/java/com/ctre/phoenix6/sim/TalonFXSimState.html
- **CANcoder SimState API**: https://api.ctr-electronics.com/phoenix6/stable/java/com/ctre/phoenix6/sim/CANcoderSimState.html
- **WPILib Units Library**: https://docs.wpilib.org/en/stable/docs/software/basic-programming/java-units.html

## Future Enhancements

1. **Gravity Compensation**: Model cosine gravity effect for arm angle
2. **Load Simulation**: Simulate holding specific loads at different angles
3. **Velocity Ramping**: More realistic motor acceleration profiles
4. **MotionMagic Tuning**: Test PID gains in simulation before deploying to hardware
5. **Advanced Physics**: Integrate WPILib's `DCMotorSim` with proper state-space modeling

## Troubleshooting

### Position Not Updating
- Check that `simulationPeriodic()` is being called in `Robot.simulationPeriodic()`
- Verify `Utils.isSimulation()` returns true (only works in simulation)
- Check motor voltage is being set via control commands

### Unrealistic Movement
- Adjust `MOTOR_INERTIA` - higher = slower acceleration
- Adjust friction threshold (0.05V) - higher = needs more voltage to move
- Adjust damping (0.99) - lower = more velocity loss per cycle

### Encoder Not Matching Motor
- Ensure CANcoder is configured as remote feedback in hardware
- Check that both `setRawRotorPosition()` and encoder position are updated
- Verify gear ratio is consistent between motor and encoder calculations
