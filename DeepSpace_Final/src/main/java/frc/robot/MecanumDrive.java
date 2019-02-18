package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class MecanumDrive {
  private TalonSRX _hatchE;
  private TalonSRX _hatchP;
  private TalonSRX _ballE;
  private TalonSRX _ballP;

  private boolean _isReversed = false;

  // PID
  private static final double kMulti = 1.0;
  private static final int kTimeout_ms = 30;
  private static final int kPidIdx = 0;
  private static final double kTargetVel_rpm = 500.0;
  private static final double kRpm_ms = 600.0;

  // TODO: Get these for each wheel from encoder
  private static final double kHatchElec_tpr = 4096.0; // ticks per rotation
  private static final double kHatchPneu_tpr = 4096.0;
  private static final double kBallElec_tpr = 4096.0;
  private static final double kBallPneu_tpr = 4096.;

  public MecanumDrive(int hePort, int hpPort, int bePort, int bpPort) {
    _hatchE = new TalonSRX(hePort);
    _hatchP = new TalonSRX(hpPort);
    _ballE = new TalonSRX(bePort);
    _ballP = new TalonSRX(bpPort);
  }

  public void init() {
    _hatchE.configFactoryDefault();
    _hatchP.configFactoryDefault();
    _ballP.configFactoryDefault();
    _ballE.configFactoryDefault();

    _hatchE.setInverted(true);
    _ballP.setInverted(true);

    _hatchE.setSensorPhase(true);
    _ballP.setSensorPhase(true);

    // TODO: tune this!
    setupPID(_hatchE, -0.181, -0.22, 0.0, 0.0);
    setupPID(_hatchP, -0.1705, -0.11, 0.0, 0.0);
    setupPID(_ballE, -0.171, -0.22, 0.0, 0.0);
    setupPID(_ballP, -0.168, -0.17, 0.0, 0.0);
  }

  public void drive(double driveX, double driveY, double look) {
    // true is PID enabled
    DriveControl controller = calculatePower(driveX, driveY, look, true);

    _hatchE.set(ControlMode.Velocity, controller.getHatchElec());
    _ballE.set(ControlMode.Velocity, controller.getBallElec());
    _hatchP.set(ControlMode.Velocity, controller.getHatchPn());
    _ballP.set(ControlMode.Velocity, controller.getBallPn());
  }

  public void driveDisabledPID(double driveX, double driveY, double look) {
    // false means PID is disabled
    DriveControl controller = calculatePower(driveX, driveY, look, false);

    _hatchE.set(ControlMode.PercentOutput, controller.getHatchElec());
    _ballE.set(ControlMode.PercentOutput, controller.getBallElec());
    _hatchP.set(ControlMode.PercentOutput, controller.getHatchPn());
    _ballP.set(ControlMode.PercentOutput, controller.getBallPn());
  }

  public void reverse() {
    _isReversed = (!_isReversed);
  }

  public boolean isReversed() {
    return _isReversed;
  }

  private void setupPID(TalonSRX talon, double kf, double p, double i, double d) {
    talon.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, kPidIdx, kTimeout_ms);

    talon.configNominalOutputForward(0.0, kTimeout_ms);
    talon.configNominalOutputReverse(0.0, kTimeout_ms);
    talon.configPeakOutputForward(1.0, kTimeout_ms);
    talon.configPeakOutputReverse(-1.0, kTimeout_ms);

    _hatchE.config_kF(kPidIdx, kf, kTimeout_ms);
    _hatchE.config_kP(kPidIdx, p, kTimeout_ms);
    _hatchE.config_kI(kPidIdx, i, kTimeout_ms);
    _hatchE.config_kD(kPidIdx, d, kTimeout_ms);
  }

  private double getMulti(boolean pidEnabled, double talonTpr) {
    double finalMulti = 1.0;
    finalMulti *= _isReversed ? -kMulti : kMulti;
    finalMulti *= pidEnabled ? kTargetVel_rpm * talonTpr / kRpm_ms : 1.0;
    return finalMulti;
  }

  private DriveControl calculatePower(double driveX, double driveY, double look, boolean pidEnabled) {
    driveX = Utils.applyDeadband(Utils.limit(driveX));
    driveY = Utils.applyDeadband(Utils.limit(driveY));

    double hatchElecVel = getMulti(pidEnabled, kHatchElec_tpr) * (driveY + driveX + look);
    double ballElecVel = getMulti(pidEnabled, kBallElec_tpr) * (-driveY + driveX - look);
    double hatchPneVel = getMulti(pidEnabled, kHatchPneu_tpr) * (driveY - driveX - look);
    double ballPneVel = getMulti(pidEnabled, kBallPneu_tpr) * (-driveY - driveX + look);

    return new DriveControl(hatchElecVel, ballElecVel, hatchPneVel, ballPneVel);
  }

  public void debug() {
    SmartDashboard.putString("MECANUM", "");
    SmartDashboard.putBoolean("isDriveReversed?", isReversed());

    SmartDashboard.putString("PID", "");
    debugTalon("Hatch Electric", _hatchE);
    debugTalon("Ball Electric", _ballE);
    debugTalon("Hatch Pneumatic", _hatchP);
    debugTalon("Ball Pneumatic", _ballP);
  }

  private void debugTalon(String key, TalonSRX talon) {
    SmartDashboard.putNumber(key + " MC Actual", talon.getSelectedSensorVelocity());
    SmartDashboard.putNumber(key + " MC Target", talon.getClosedLoopTarget());
    SmartDashboard.putNumber(key + " MC Error", talon.getClosedLoopError());
  }
}