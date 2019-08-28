Q = dlmread('D:\Data\cuongvv_shockPoint_80kmh_20190810025437.txt');
%Q = dlmread('D:\Data\cuongvv_shockPoint_60kmh_20190810120808.txt');
%Q = dlmread('D:\Data\cuongvv_shockPoint_90kmh_20190810030301.txt');

xA = Q(:,2);
yA = Q(:,3); 
zA = Q(:,4);
nA = length(xA);
WA = zeros(100,1);
avgSum = 0; 
for i = 1:nA
    WA(i,1) = sqrt(xA(i,1)^2 + yA(i,1)^2 + zA(i,1)^2);
end
for j = 100:199
    avgSum = avgSum + WA(j,1);
end
avgSum = avgSum/100;

A = dlmread('D:\Data\bigdata.txt');
x = A(:,2);
y = A(:,3);
z = A(:,4);
latitude = A(:,5);
longitude = A(:,6);
n = length(x);
sum=0;
i=0;
while i < n-101
    W = zeros(100,1);
    for j = 1:100
        W(j,1) = sqrt(x(i+j,1)^2 + y(i+j,1)^2 + z(i+j,1)^2)/avgSum;
    end
    W_cv =W';
    X = fft(W_cv);%bien doi Fourier khong nam o trung tam
    N = length(W_cv); %Xac dinh kich thuoc cua x
    a = 5; %tan so cat
    w = (-N/2+1:(N/2)); % Vector tan so trung tam
    H = a./(a + 1i*w);  %H nam o trung tam
    Hshift = fftshift(H); %H khong nam o trung tam
    Y = X.*Hshift ;  % loc tin hieu
    g = real(ifft(Y)); %bien doi Fourier nguoc 
    %figure;
    %plot(g);
    for h = 1:100
        if g(1,h)>5.5
            sum=sum+1;
            disp(i+h);
            disp(latitude(i+h,1));
            disp(longitude(i+h,1));
            i=i+120;
            break;
        end
    end
    i=i+1;
end
disp(sum);
