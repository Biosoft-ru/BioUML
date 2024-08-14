function[Energy]=hemodynamicModelSimulation()
Nfun=2;%***********number of unknown functions******************************
M=10;%*************segmentation for integrating*****************************
%******************physical parameters**************************************
rho=1;
h_=0.05;
nu=0.035;
E=3*10^6;

N=10; %***********number of segmentation***********************************
tau=0.1; %*********time step************************************************
t=0; %***********start time***********************************************
T=4; %***********end time*************************************************
Nvet=5; %********number of vessels****************************************
%******************vessels length*******************************************
length(1)=14.0;
length(2)=12.0;
length(3)=13.4;
length(4)=13.4;
length(5)=17.7;

%******************vessels initial area*************************************
A0(1)=5.983;
A0(2)=5.147;
A0(3)=1.219;
A0(4)=0.562;
A0(5)=0.432;

%******************derived physical parameters******************************
gamma=E*h_*sqrt(pi);
Kr=8*pi*nu;
%******************segmentation*********************************************
for j=1:Nvet,
    h(j)=length(j)/N;
end;
%******************dimension************************************************
n_a=Nfun*Nvet;
%******************solution*************************************************
V=zeros([Nfun,(N+1),Nvet]);
for i=1:N+1,
    for j=1:Nvet,
        V(1,i,j)=A0(j);
    end;
end;
%*************time iteration************************************************
Kmax=round(T/tau);
Energy=zeros(Kmax);
for k=1:Kmax,
    t=t+tau;                                                      
    Ordinata(k)=t;
    %*************marching *************************************************
    %*************left boundary condition***********************************
    for j=1:Nvet,
        An(j)=V(1,1,j);
        Qn(j)=V(2,1,j);
        a(j)=gamma/(A0(j)*(sqrt(An(j))+sqrt(A0(j))));
        b(j)=(rho*Qn(j))/(2*An(j)^2);
        c(j)=gamma/(sqrt(An(j))+sqrt(A0(j)));
    end;
    L=[a(1),b(1),0,0,0,0,0,0,0,0;
0,0,a(2),b(2),0,0,0,0,0,0;
0,0,0,0,a(3),b(3),-a(4),-b(4),0,0;
0,0,0,0,a(3),b(3),0,0,-a(5),-b(5);
0,0,0,0,0,1,0,-1,0,-1];
l=[c(1);c(2)+70   ;c(3)-c(4);c(3)-c(5);0];
    [n_l,n]=size(L);
    %*************right boundary condition**********************************
    for j=1:Nvet,
        An(j)=V(1,N+1,j);
        Qn(j)=V(2,N+1,j);
        a(j)=gamma/(A0(j)*(sqrt(An(j))+sqrt(A0(j))));
        b(j)=(rho*Qn(j))/(2*An(j)^2);
        c(j)=gamma/(sqrt(An(j))+sqrt(A0(j)));
    end;
    c(1)=c(1)+0.3*abs(sin(6*t));
    %c(2)=c(2)+0.1*abs(sin(4*t));
    %c(3)=c(3)+0.2*abs(sin(6*t));
    R=[a(1),b(1),-a(2),-b(2),0,0,0,0,0,0;
a(1),b(1),0,0,-a(3),-b(3),0,0,0,0;
0,1,0,-1,0,-1,0,0,0,0;
0,0,0,0,0,0,a(4),b(4),0,0;
0,0,0,0,0,0,0,0,a(5),b(5)];
r=[c(1)-c(2);c(1)-c(3);0;c(4)+70   ;c(5)+70   ];
    %*************assignment ***********************************************
    n_od=n-n_l;% number of linealy independent solutions homogeneous left boundary condition for one vessel
    Z_0=zeros([n,n_od,N+1]);
    Z_f=zeros([n,N+1]);
    Y_0=zeros([n,n_od,N+1]);
    Y_f=zeros([n,N+1]);
    Ort=zeros([n_od+1,n_od+1,N]);%orthogonalising matrices
    U=zeros([Nfun,N+1,Nvet]); %solution
    beta=zeros([n_od+1,N+1]);%coefficients of return marching
    %*************processing of left boundary condition*********************
    [Q,R_left]=qr(L');
    Q_trans=Q';
    RR=(R_left(1:n_l,1:n_l))';
    Y_f(1:n_l,1)=inv(RR)*l;
    Y_f(:,1)=Q*Y_f(:,1);
    Y_0(:,:,1)=Q(:,n_l+1:n);
    Z_0(:,:,1)=Y_0(:,:,1);
    Z_f(:,1)=Y_f(:,1);
    %**************straight marching****************************************
    for j=1:Nvet;
        q=V(2,1,j); 
        a=V(1,1,j);
        c=(gamma*sqrt(a))/(2*rho*A0(j));
        G1(:,:,j)=-1/tau*[2*q/(q^2/a-c*a), 1/(-(q/a)^2+c);
            1,0];
        f1(:,j)=[q*(1/tau+Kr/a)/((q/a)^2-c);
            a/tau];
    end;
    %**************dimensional segmentation loop****************************
    for i=1:N, 
        % vessels loop
        for j=1:Nvet 
            q=V(2,i+1,j); 
            a=V(1,i+1,j);
            c=(gamma*sqrt(a))/(2*rho*A0(j));
            G2(:,:,j)=-1/tau*[2*q/(q^2/a-c*a), 1/(-(q/a)^2+c);
                1,0];
            f2(:,j)=[q*(1/tau+Kr/a)/((q/a)^2-c);
                a/tau];    

            % solution of Cauchy problem
            del=h(j)/M;
            del_mat=(G2(:,:,j)-G1(:,:,j))/M;
            del_fun=(f2(:,j)-f1(:,j))/M;
            z_0=Z_0((j-1)*Nfun+1:j*Nfun,:,i);
            z_f=Z_f((j-1)*Nfun+1:j*Nfun,i);
            % integration
            for jj=1:M,
                A=G1(:,:,j)+(jj-1/2)*del_mat; % intermediate matrix
                ff=f1(:,j)+(jj-1/2)*del_fun; % intermediate right part
                del_0=A*z_0*del;
                z_0=z_0+del_0; %solution of homogeneous equations
                del_f=(A*z_f+ff)*del;
                z_f=z_f+del_f; %solution of nonhomogeneous equations
            end;
            % new value
            Y_0((j-1)*Nfun+1:j*Nfun,:,i+1)=z_0;
            Y_f((j-1)*Nfun+1:j*Nfun,i+1)=z_f; 
        end;
        % orthogonalizaiton
        Y=[Y_0(:,:,i+1),Y_f(:,i+1)];
        [Qpr,Rpr]=qr(Y);
        Z_f(:,i+1)=Qpr(:,n_od+1)*Rpr(n_od+1,n_od+1);
        Rpr(n_od+1,n_od+1)=1;
        Z_0(:,:,i+1)=Qpr(:,1:n_od);
        Ort(:,:,i)=Rpr(1:(n_od+1),1:(n_od+1));
    end;

    %***************inversion of right boundary condition*******************
    alpha=inv(R*Z_0(:,:,N+1))*(r-R*Z_f(:,N+1));
    %***************return marching*****************************************
    beta(:,N+1)=[alpha;1];
    Z(:,N+1)=[Z_0(:,:,N+1),Z_f(:,N+1)]*beta(:,N+1);
    if N>1,
        for i=1:N,
            m=N+1-i;
            beta(:,m)=inv(Ort(:,:,m))*beta(:,m+1);
            Z(:,m)=[Z_0(:,:,m),Z_f(:,m)]*beta(:,m);
        end;
    else
        beta(:,1)=inv(Ort(:,:,1))*beta(:,2);
    end;
    Z(:,1)=[Y_0(:,:,1),Y_f(:,1)]*beta(:,1);

    %***************vessels loop********************************************
    for j=1:Nvet,
        U(:,:,j)=Z((j-1)*Nfun+1:j*Nfun,:); %solution
    end;
    %***************reserved operators for output result********************
    UUL=[U(:,1,1)];
    for i=1:Nvet-2,
        UUL=[UUL(1:2*i);U(:,1,i+1)];
    end;
    UUL=[UUL(1:2*Nvet-2);U(:,1,Nvet)];
    UUR=[U(:,N+1,1)];
    for i=1:Nvet-2,
        UUR=[UUR(1:2*i);U(:,N+1,i+1)];
    end;
    UUR=[UUR(1:2*Nvet-2);U(:,N+1,Nvet)];
    WL=L*UUL-l;
    WR=R*UUR-r;
    Lbound(k)=norm(WL);
    Rbound(k)=norm(WR);
    %***************move to next step***************************************
    V=U;
    % saving results
    for j=1:Nvet
        OA(k,:,j)=U(1,:,j);
        OQ(k,:,j)=U(2,:,j);
    end;   

end;

%**********graphing**********************************************************

for k=1:Kmax,
    for i=1:N+1,
        OA2back(k,i)=OA(k,N+2-i,2);
        OA3back(k,i)=OA(k,N+2-i,3);
        OQ2back(k,i)=OQ(k,N+2-i,2);
        OQ3back(k,i)=OQ(k,N+2-i,3);
    end;  
end;

for i=1:(N+1),
    for j=1:Nvet,
        Abszissa(i,j)=(i-1)*h(1);
    end;
end;

for i=1:(N+1),
    Abszissa_dop(i)=l(1)+(i-1)*h(2);
end;

F1=figure;% area of  1 vessel(time,length)
%F2=figure;
%F3=figure;
F4=figure;% flow through 1 vessel(time,length)
%F5=figure;
%F6=figure;


figure(F1); hold;
surf( Abszissa(:,1),Ordinata, OA(:,:,1)); hold; pause;
figure(F1); hold;
%figure(F2); hold;
%surf( Abszissa(:,2),Ordinata, OA2back); hold;pause;
%figure(F3); hold;
%surf( Abszissa(:,3),Ordinata, OA3back); hold;pause;
figure(F4); hold;
surf( Abszissa(:,1),Ordinata, OQ(:,:,1)); hold; pause;
%figure(F5); hold;
%surf( Abszissa(:,2),Ordinata, OQ2back); hold;pause;
%figure(F6); hold;
%surf( Abszissa(:,3),Ordinata, OQ3back); hold;pause;

disp(max(Lbound));
disp(max(Rbound)); pause;

Energy=1;
