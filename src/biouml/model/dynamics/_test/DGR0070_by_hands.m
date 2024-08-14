function [t, y] = DGR0070_by_hands()

global k1 k2_ k3 k4 k5 k6_ k7 k7r k8 k8r kc kcr ki kir kp ku kur ku2 kur2 kw kwr V2 V2_ V6 V6_ V25 V25_ Vw Vw_ mu K_mc K_mcr K_mi K_mir K_mp K_mu K_mur K_mu2 K_mur2 K_mw K_mwr alpha beta Cig1 

k1 = 0.015
k2_ = 0.05
k3 = 0.09375
k4 = 0.1875
k5 = 0.00175
k6_ = 0
k7 = 100
k7r = 0.1
k8 = 10
k8r = 0.1
kc = 1
kcr = 0.25
ki = 0.4
kir = 0.1
kp = 3.25
ku = 0.2
kur = 0.1
ku2 = 1
kur2 = 0.3
kw = 1
kwr = 0.25
V2 = 0.25
V2_ = 0.0075
V6 = 7.5
V6_ = 0.0375
V25 = 0.5
V25_ = 0.025
Vw = 0.35
Vw_ = 0.035
mu = 0.00495

K_mc = 0.1
K_mcr = 0.1
K_mi = 0.01
K_mir = 0.1
K_mp = 0.001
K_mu = 0.01
K_mur = 0.01
K_mu2 = 0.05
K_mur2 = 0.05
K_mw = 0.1
K_mwr = 0.1
alpha = 0.25
beta = 0.05
Cig1 = 0

y = []
y(1) = 0.0                              % y(1) - G2K
y(2) = 0.1                              % y(2) - R
y(3) = 0.05                             % y(3) - G1K
y(4) = 0.05                              % y(4) - G2R
y(5) = 0.05                              % y(5) - IE
y(6) = 0.05                              % y(6) - UbE2
y(7) = 0.05                              % y(7) - Wee1
y(8) = 0.05                              % y(8) - PG2
y(9) = 0.05                              % y(9) - G1R
y(10) = 0.05                             % y(10) - PG2R
y(11) = 0.2                             % y(11) - UbE
y(12) = 0.05                             % y(12) - mass
y(13) = 0.05                             % y(13) - Cdc25

%plot the solver output
%title ('Solving DGR0070 - Modelling the Control of DNA Replication in Fission Yeast problem')
%ylabel ('y(t)')
%xlabel ('x(t)')
%legend('G2K', 'R', 'G1K', 'G2R', 'IE', 'UbE2', 'Wee1', 'PG2', 'G1R', 'PG2R', 'UbE', 'mass', 'Cdc25');

tstart = 0.0
tfinal = 2

tout   = tstart;
yout   = y.';
teout  = [];    % time vector when events have occured
yeout  = [];    % solution at corresponding time
ieout  = [];    % index of event that have occured
states = [];    % states is subset from events

global transition_ready current_state
current_state = char('G1');
transition_ready(1:4) = 0;

options = odeset('Events', @events, 'OutputFcn', @odeplot2);

% ------------------------------------------------
figure;
%set(gca,'xlim',[tstart tfinal], 'ylimmode', 'manual');
set(gca,'xlim',[tstart tfinal], 'ylim', [0 0.2]);
box on
hold on;
% ------------------------------------------------

y0 = y;

while tstart < tfinal
    te = tfinal;
    % find the earliest time of transition execution.
    [te, transition_after_number] = nextTransitionExecution(tstart, tfinal, y);
    [t,y,te,ye,ie] = ode23(@DGR0070_by_hands_dydt, [tstart te], y0, options);

    % Accumulate output.  This could be passed out as output arguments.
    nt = length(t);
    tout = [tout; t(2:nt)];
    yout = [yout y(2:nt,:)'];
    y0 = y(nt,:);
    % accumulating events
    teout = [teout; te];          % Events at tstart are never reported. 
    yeout = [yeout; ye];
    ieout = [ieout; ie];
    tstart = t(nt); 

    if length(ie) == 1
        y0 = processEvent( ie(1), te, ye )
    else
        if transition_after_number == -1
            break;
        else
            y0 = processAfterExecution( t(nt), tfinal, y0, transition_after_number )
        end;
    end
end
t = tout;
y = yout';


%----------------------------------------------------------------------

function dydt = DGR0070_by_hands_dydt(t, y)
% Calculates dy/dt for model.

global k1 k2_ k3 k4 k5 k6_ k7 k7r k8 k8r kc kcr ki kir kp ku kur ku2 kur2 kw kwr V2 V2_ V6 V6_ V25 V25_ Vw Vw_ mu K_mc K_mcr K_mi K_mir K_mp K_mu K_mur K_mu2 K_mur2 K_mw K_mwr alpha beta Cig1 

    k2 = V2_*(1-y(11));
    k6 = V6_*(1-y(6))+V6*y(6);
    kwee = Vw_*(1 - y(7)) + Vw*y(7);
    k25 = V25_*(1-y(13)) + V25*y(13);

    MPF = y(1) + beta*y(8);

    SPF = MPF + alpha*y(3) + Cig1;

% calculates dy/dt for 'diagram' model
dydt = []

dydt(1) =  k1 - (k2+kwee + k7*y(2))*y(1) + k25*y(8) + (k7r+k4)*y(4);
dydt(2) =  k3 - k4*y(2) - kp*y(2)*SPF*y(12)/(K_mp + y(2)) - k7*y(2)*(y(1)+y(8))+(k7r+k2+k2_)*(y(4)+y(10)) - k8*y(2)*y(3)+(k8r+k6_)*y(9);
dydt(3) =  k5-(k6+k8*y(2))*y(3) + (k8r+k4)*y(9);
dydt(4) =  k7*y(2)*y(1) - (k7r + k4+k2+k2_)*y(4);
dydt(5) =  (ki*MPF*(1-y(5)))/(K_mi+1-y(5)) - kir*y(5)/(K_mir+y(5));
dydt(6) =  (ku2*MPF*(1-y(6)))/(K_mu2+1-y(6))-(kur2*y(6))/(K_mur2+y(6));
dydt(7) =  (kwr*(1-y(7)))/(K_mwr+1-y(7)) - (kw*MPF*y(7))/(K_mw+y(7));
dydt(8) =  kwee*y(1)-(k25+k2+k7*y(2))*y(8) + (k7r+k4)*y(10);
dydt(9) =  k8*y(2)*y(3)-(k8r+k4+k6_)*y(9);
dydt(10) = k7*y(2)*y(8)-(k7r+k4+k2+k2_)*y(10);
dydt(11) = ku*y(5)*(1-y(11))/(K_mu+1-y(11))-kur*y(11)/(K_mur+y(11));
dydt(12) = mu*y(12);
dydt(13) = kc*MPF*(1-y(13))/(K_mc+1-y(13))-(kcr*y(13))/(K_mcr+y(13));

dydt = dydt';

     
function [value,isterminal,direction] = events(t,y) 

global k1 k2_ k3 k4 k5 k6_ k7 k7r k8 k8r kc kcr ki kir kp ku kur ku2 kur2 kw kwr V2 V2_ V6 V6_ V25 V25_ Vw Vw_ mu K_mc K_mcr K_mi K_mir K_mp K_mu K_mur K_mu2 K_mur2 K_mw K_mwr alpha beta Cig1 

    MPF = y(1) + beta*y(8)

    SPF = MPF + alpha*y(3) + Cig1

    value(1:1) = -1;
	
    if y(11) <= 0.1
        value(1) = +1;
    end

	if SPF >= 0.1 
		transition_ready(1) = 1;
	end

    isterminal(1:1) = 1;
    direction(1:1) = 1;


function y = processEvent(event, time, y)
%constants declaration

global k1 k2_ k3 k4 k5 k6_ k7 k7r k8 k8r kc kcr ki kir kp ku kur ku2 kur2 kw kwr V2 V2_ V6 V6_ V25 V25_ Vw Vw_ mu K_mc K_mcr K_mi K_mir K_mp K_mu K_mur K_mu2 K_mur2 K_mw K_mwr alpha beta Cig1 

global transition_ready current_state
    % WARNING: In general, order of transitions is not defined 
    %          if several of them are ready to occure.
    switch char(current_state)
        case 'G1'
            if transition_ready(1) == +1
                current_state = char('S');
            end;

        case 'G2'
        case 'M'
        case 'S'
    end;

	switch event
		case 1
			y(12) = y(12)/2;
			kp = kp*2;
			
    end
    transition_ready(1:4) = 0;

%----------------------------------------------------------------------

function y = processAfterExecution(tend, tfinal, y, transition_after_number)
%constants declaration

global k1 k2_ k3 k4 k5 k6_ k7 k7r k8 k8r kc kcr ki kir kp ku kur ku2 kur2 kw kwr V2 V2_ V6 V6_ V25 V25_ Vw Vw_ mu K_mc K_mcr K_mi K_mir K_mp K_mu K_mur K_mu2 K_mur2 K_mw K_mwr alpha beta Cig1 

global current_state
    switch char(current_state)
        case 'G1'
        case 'G2'
            if transition_after_number == 2
                % then execute assignments of state'M'.
                current_state = char('M');
            end;

        case 'M'
            if transition_after_number == 3
                % first execute exit assignments of state 'M'.
                y(1) = y(1)/2;
                current_state = char('G1');
            end;

        case 'S'
            if transition_after_number == 4
				kp = kp/2;
                current_state = char('G2');
            end;

    end;


function [t, transition_after_number] = nextTransitionExecution(tstart, tfinal, y)
%constants declaration

global k1 k2_ k3 k4 k5 k6_ k7 k7r k8 k8r kc kcr ki kir kp ku kur ku2 kur2 kw kwr V2 V2_ V6 V6_ V25 V25_ Vw Vw_ mu K_mc K_mcr K_mi K_mir K_mp K_mur K_mu K_mu2 K_mur2 K_mw K_mwr alpha beta Cig1 

global current_state
    t = tfinal;
    transition_after_number = -1;
    period = tfinal - tstart;
    switch char(current_state)
        case 'G1'
        case 'G2'
            temp = 60;
            if tstart + temp < tfinal & temp < period
                t = tstart + temp;
                period = temp;
                transition_after_number = 2;
            end
        case 'M'
            temp = 120;
            if tstart + temp < tfinal & temp < period
                t = tstart + temp;
                period = temp;
                transition_after_number = 3;
            end
        case 'S'
            temp = 120;
            if tstart + temp < tfinal & temp < period
                t = tstart + temp;
                period = temp;
                transition_after_number = 4;
            end
    end
