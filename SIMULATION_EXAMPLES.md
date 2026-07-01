# ArmSubsystem Simulation Examples

## Running Simulation in VS Code

### Step 1: Build and Run Simulation
```bash
# In VS Code Terminal:
./gradlew simulateJava
```

### Step 2: Test Commands
Once the simulator GUI loads, you can:
- Click dashboard buttons to trigger arm commands
- Observe arm position changes in the simulator console
- Monitor rotor position and velocity outputs

## Physics Model Parameters

### Understanding Current Configuration

```java
GEAR_RATIO = 10.0
```
- **Meaning**: Motor shaft rotates 10 times for 1 arm mechanism rotation
- **Effect**: Higher ratio = slower but stronger arm
- **Adjust for**: Your actual gearbox reduction

```java
MOTOR_INERTIA = 0.001
```
- **Units**: kg⋅m²
- **Effect**: Higher inertia = harder to accelerate/decelerate
- **Typical Values**:
  - 0.0001: Very responsive (light rotor)
  - 0.001: Moderate response (KrakenX60 typical)
  - 0.01: Heavy, slow response (loaded mechanism)

```java
friction = 0.05
```
- **Units**: Volts
- **Meaning**: Motor won't move until voltage exceeds 0.05V
- **Adjust for**: Your motor's static friction
- **Typical Values**:
  - 0.02: Very smooth motor
  - 0.05: Standard gearmotor
  - 0.1: High-friction system

```java
damping (0.99)
```
- **Meaning**: Retain 99% of velocity each simulation cycle
- **Effect**: Lower value = more speed loss (more friction)
- **Typical Values**:
  - 0.95: Very high friction
  - 0.99: Moderate friction
  - 0.999: Very smooth bearing

## Common Simulation Testing Patterns

### 1. Testing Position Control

```java
// Test horizontal position command
public void testHorizontalPosition() {
    armSubsystem.Horizontal().execute();
    // In simulation, position should move toward 2.0 rotations
}

// Test vertical position command  
public void testVerticalPosition() {
    armSubsystem.Vertical().execute();
    // In simulation, position should move toward 1.0 rotations
}
```

### 2. Testing Stop Command

```java
public void testStop() {
    armSubsystem.Horziontal().execute();  // Move
    armSubsystem.stopCommand().execute();  // Stop
    // Position should hold at current value
}
```

### 3. Monitoring Simulation State

```java
// In simulationPeriodic() or test code:
public void printSimulationState() {
    System.out.println("Position: " + simPosition + " rotations");
    System.out.println("Velocity: " + simVelocity + " rot/sec");
    System.out.println("Motor Voltage: " + motorVoltage.in(Volts) + " V");
}
```

## Tuning Guide

### If Arm Accelerates Too Slowly
Try these changes:
```java
// Option 1: Reduce inertia
MOTOR_INERTIA = 0.0005;  // Was 0.001

// Option 2: Reduce friction
friction = 0.02;  // Was 0.05

// Option 3: Increase damping (less friction)
simVelocity *= 0.995;  // Was 0.99
```

### If Arm Overshoots Position Target
Try these changes:
```java
// Option 1: Increase inertia
MOTOR_INERTIA = 0.005;  // Was 0.001

// Option 2: Increase friction
friction = 0.1;  // Was 0.05

// Option 3: Decrease damping (more friction)
simVelocity *= 0.98;  // Was 0.99
```

### If Arm Won't Move at All
Check these:
1. Verify control command is setting voltage (not just position)
2. Check that supply voltage is above friction threshold
3. Ensure `simulationPeriodic()` is called from `Robot.simulationPeriodic()`
4. Verify motor type is set to `KrakenX60`

## Advanced: Gravity Compensation Testing

Current implementation doesn't include gravity, but you can test it by:

```java
// Estimated gravity effect: F_gravity = kG * cos(angle)
// For now, static friction models basic arm behavior
// Add this for more realistic gravity:

double armAngle = simPosition * 360.0 / GEAR_RATIO;  // Degrees
double gravityTorque = 0.5 * Math.cos(Math.toRadians(armAngle));  // N⋅m
double gravityVoltage = gravityTorque * 12.0 / 100.0;  // Convert to voltage

// Subtract from available motor voltage in calculations
double availableVoltage = motorVoltageValue - gravityVoltage;
double acceleration = (availableVoltage - friction) / MOTOR_INERTIA;
```

## Debugging Simulation Issues

### Enable Verbose Logging

Add to `ArmSubsystem.simulationPeriodic()`:

```java
private int printCounter = 0;

public void simulationPeriodic() {
    // ... existing code ...
    
    // Print every 10 cycles (about 200ms)
    printCounter++;
    if (printCounter >= 10) {
        printCounter = 0;
        System.out.printf("Arm Sim - Pos: %.3f rot, Vel: %.3f rot/s, Volt: %.2f V%n",
            simPosition, simVelocity, motorVoltageValue);
    }
}
```

### Check SimState Values

```java
// In Robot.simulationPeriodic() after arm update:
var armSim = m_robotContainer.getArmSubsystem().armSimState;
System.out.println("Raw Rotor Position: " + armSim.getRawRotorPosition());
System.out.println("Rotor Velocity: " + armSim.getRotorVelocity());
```

## Dashboard Integration

The simulated arm position can be viewed on:
1. **NetworkTables Viewer**: Shows `arm/position` and `arm/velocity`
2. **Glass Dashboard**: Real-time graph of arm motion
3. **Shuffleboard**: Custom layouts for arm control

### Example Shuffleboard Widget

```java
// In Robot class or dashboard setup:
Shuffleboard.getTab("Arm")
    .add("Position", 0.0)
    .withPosition(0, 0)
    .withSize(2, 1);
```

## Performance Considerations

The simulation physics runs at 50 Hz (20ms cycles) in FRC:
- **Euler integration** is simple but good enough for FRC
- **Physics constants** affect accuracy:
  - More realistic: Smaller MOTOR_INERTIA, More damping
  - Faster simulation: Larger MOTOR_INERTIA, Less damping
- **No impact on hardware**: Simulation code only runs when `Utils.isSimulation()` is true

## Next Steps

1. **Measure Real Arm**: Get actual values for:
   - Motor inertia (from motor specs)
   - Gear ratio (from mechanical design)
   - Static friction (empirically test)

2. **Validate Simulation**:
   - Compare simulated motion to real arm motion
   - Tune constants until simulation matches hardware

3. **Test Controllers**:
   - Run PID tuning in simulation
   - Test command sequences before hardware deployment

4. **Add Features**:
   - Gravity compensation model
   - Load simulation
   - Limit switch simulation
   - Current limiting
