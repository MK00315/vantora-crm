import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, ArrowRight, Check, Eye, EyeOff, LoaderCircle, Mail, ShieldCheck, Sparkles } from 'lucide-react';
import { useEffect, useState, type PropsWithChildren } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { z } from 'zod';
import { authApi } from '@/api/services';
import { useAuth } from '@/auth/AuthContext';
import { Button, Field, Input } from '@/components/ui';
import { useToast } from '@/components/ui/Toast';
import { getApiMessage } from '@/lib/utils';

function Brand() {
  return <Link to="/" className="inline-flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-lg bg-gradient-to-br from-brand-600 to-sky-700 text-sm font-bold text-white shadow-sm ring-1 ring-white/50">V</div><span className="font-display text-xl font-bold tracking-tight text-ink-900 dark:text-white">Vantora CRM</span></Link>;
}

function AuthLayout({ children, kicker, title, description }: PropsWithChildren<{ kicker: string; title: string; description: string }>) {
  return (
    <div className="min-h-screen bg-sand lg:grid lg:grid-cols-[1.05fr_.95fr]">
      <section className="enterprise-gradient relative hidden min-h-screen overflow-hidden border-r border-ink-200 p-12 lg:flex lg:flex-col xl:p-16">
        <div className="absolute inset-0 subtle-grid opacity-70" /><div className="absolute -left-48 top-28 h-[30rem] w-[30rem] rounded-full bg-brand-200/35 blur-[110px]" /><div className="absolute -right-40 bottom-10 h-96 w-96 rounded-full bg-sky-200/35 blur-[100px]" />
        <div className="relative z-10"><Brand /></div>
        <div className="relative z-10 my-auto max-w-xl"><div className="mb-6 inline-flex items-center gap-2 rounded-full border border-brand-200 bg-white/80 px-3 py-1.5 text-xs font-semibold text-brand-700 shadow-sm"><Sparkles className="h-3.5 w-3.5" />Built for focused customer teams</div><h1 className="font-display text-4xl font-bold leading-[1.12] tracking-[-.035em] text-ink-950 xl:text-5xl">A clearer way to run every <span className="bg-gradient-to-r from-brand-700 to-sky-700 bg-clip-text text-transparent">customer relationship.</span></h1><p className="mt-6 max-w-lg text-base leading-7 text-ink-600">Bring your pipeline, customers, tasks, and team into one secure workspace designed for daily operational clarity.</p><div className="mt-9 grid gap-3">{['Tenant-isolated company data', 'Role-based access for every team', 'Traceable workflows and activity'].map((label) => <div key={label} className="flex items-center gap-3 rounded-xl border border-white/90 bg-white/75 px-4 py-3 text-sm font-medium text-ink-700 shadow-sm backdrop-blur"><span className="grid h-7 w-7 place-items-center rounded-full bg-brand-100 text-brand-700"><Check className="h-4 w-4" /></span>{label}</div>)}</div></div>
        <p className="relative z-10 text-xs text-ink-500">© {new Date().getFullYear()} Vantora CRM. Built around your customer.</p>
      </section>
      <section className="relative flex min-h-screen items-center justify-center overflow-hidden bg-white/60 px-5 py-8 dark:bg-ink-950 sm:px-8">
        <div className="absolute right-[-10rem] top-[-8rem] h-80 w-80 rounded-full bg-sky-100/70 blur-3xl dark:hidden" /><div className="absolute bottom-[-10rem] left-[-8rem] h-80 w-80 rounded-full bg-brand-100/70 blur-3xl dark:hidden" />
        <div className="relative w-full max-w-lg animate-fade-up"><div className="mb-7 lg:hidden"><Brand /></div><div className="rounded-2xl border border-ink-200 bg-white p-6 shadow-lift dark:border-ink-800 dark:bg-ink-900 sm:p-8"><p className="text-xs font-semibold uppercase tracking-[.16em] text-brand-700 dark:text-brand-400">{kicker}</p><h2 className="mt-3 font-display text-3xl font-bold tracking-tight text-ink-950 dark:text-white">{title}</h2><p className="mt-3 text-sm leading-6 text-ink-500 dark:text-ink-400">{description}</p><div className="mt-7">{children}</div></div></div>
      </section>
    </div>
  );
}

/**
 * Captures one-time secrets from the fragment so they are never sent in HTTP
 * referrers or server request lines. Query strings remain a short-lived legacy
 * fallback and are scrubbed, together with the fragment, after the first render.
 */
function useOneTimeToken() {
  const [token] = useState<string | null>(() => {
    const fragment = new URLSearchParams(window.location.hash.replace(/^#/, '')).get('token');
    return fragment || new URLSearchParams(window.location.search).get('token');
  });

  useEffect(() => {
    const url = new URL(window.location.href);
    let changed = url.searchParams.has('token');
    url.searchParams.delete('token');
    const fragment = new URLSearchParams(url.hash.replace(/^#/, ''));
    if (fragment.has('token')) changed = true;
    fragment.delete('token');
    url.hash = fragment.toString() ? `#${fragment.toString()}` : '';
    if (changed) window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`);
  }, []);

  return token;
}

const loginSchema = z.object({ email: z.email('Enter a valid work email'), password: z.string().min(8, 'Password must be at least 8 characters') });
type LoginValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const { login, isAuthenticated, loading } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [visible, setVisible] = useState(false);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } });
  if (!loading && isAuthenticated) return <Navigate to="/app" replace />;
  const submit = async (values: LoginValues) => {
    try {
      await login(values.email, values.password);
      toast('Welcome back', { description: 'Your workspace is ready.' });
      navigate('/app', { replace: true });
    } catch (error) {
      toast('Could not sign you in', { kind: 'error', description: getApiMessage(error, 'Check your email and password.') });
    }
  };
  return <AuthLayout kicker="Welcome back" title="Sign in to your workspace" description="Pick up where your team left off and keep every opportunity moving."><form onSubmit={handleSubmit(submit)} className="space-y-5" noValidate><Field label="Work email" error={errors.email?.message}><Input autoFocus autoComplete="email" placeholder="you@company.com" invalid={Boolean(errors.email)} {...register('email')} /></Field><Field label="Password" error={errors.password?.message}><div className="relative"><Input type={visible ? 'text' : 'password'} autoComplete="current-password" placeholder="Your password" className="pr-11" invalid={Boolean(errors.password)} {...register('password')} /><button type="button" onClick={() => setVisible((v) => !v)} className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-ink-400 hover:text-ink-700" aria-label={visible ? 'Hide password' : 'Show password'}>{visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}</button></div><div className="mt-2 flex items-center justify-between gap-3"><Link to="/resend-verification" className="text-xs font-semibold text-ink-500 hover:text-brand-700 dark:text-ink-400 dark:hover:text-brand-400">Need a verification link?</Link><Link to="/forgot-password" className="text-xs font-semibold text-brand-700 hover:text-brand-800 dark:text-brand-400">Forgot password?</Link></div></Field><Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>{isSubmitting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <>Continue <ArrowRight className="h-4 w-4" /></>}</Button></form><p className="mt-8 text-center text-sm text-ink-500">New to Vantora CRM? <Link to="/register" className="font-semibold text-brand-700 hover:text-brand-800 dark:text-brand-400">Create a workspace</Link></p></AuthLayout>;
}

const registerSchema = z.object({ organizationName: z.string().min(2, 'Enter your company name'), firstName: z.string().min(2, 'Enter your first name'), lastName: z.string().min(2, 'Enter your last name'), email: z.email('Enter a valid work email'), password: z.string().min(10, 'Use at least 10 characters').regex(/[a-z]/, 'Include one lowercase letter').regex(/[A-Z]/, 'Include one uppercase letter').regex(/[0-9]/, 'Include one number') });
type RegisterValues = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const { register: createAccount, isAuthenticated, loading } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterValues>({ resolver: zodResolver(registerSchema), defaultValues: { organizationName: '', firstName: '', lastName: '', email: '', password: '' } });
  if (!loading && isAuthenticated) return <Navigate to="/app" replace />;
  const submit = async (form: RegisterValues) => {
    const values = { organizationName: form.organizationName, firstName: form.firstName, lastName: form.lastName, email: form.email, password: form.password };
    try { await createAccount(values); toast('Check your email to finish setup', { description: `We sent a verification link to ${values.email}. Verify your address before signing in.` }); navigate('/login', { replace: true }); }
    catch (error) { toast('Could not create workspace', { kind: 'error', description: getApiMessage(error) }); }
  };
  return <AuthLayout kicker="Create workspace" title="Set up your company workspace" description="Create a tenant-isolated CRM workspace for your team and customer operations."><form onSubmit={handleSubmit(submit)} className="space-y-4" noValidate><Field label="Company name" error={errors.organizationName?.message}><Input placeholder="Aurora Labs" autoFocus {...register('organizationName')} /></Field><div className="grid grid-cols-1 gap-3 sm:grid-cols-2"><Field label="First name" error={errors.firstName?.message}><Input autoComplete="given-name" {...register('firstName')} /></Field><Field label="Last name" error={errors.lastName?.message}><Input autoComplete="family-name" {...register('lastName')} /></Field></div><Field label="Work email" error={errors.email?.message}><Input type="email" autoComplete="email" placeholder="you@company.com" {...register('email')} /></Field><Field label="Password" error={errors.password?.message} hint="10+ characters with upper-case, lower-case, and a number"><Input type="password" autoComplete="new-password" {...register('password')} /></Field><p className="rounded-lg border border-ink-200 bg-ink-50 px-3 py-2.5 text-xs leading-5 text-ink-500 dark:border-ink-700 dark:bg-ink-800 dark:text-ink-300">Account and workspace data is stored by the CRM deployment you are connecting to.</p><Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>{isSubmitting && <LoaderCircle className="h-4 w-4 animate-spin" />}Create my workspace</Button></form><p className="mt-7 text-center text-sm text-ink-500">Already have an account? <Link to="/login" className="font-semibold text-brand-700 dark:text-brand-400">Sign in</Link></p></AuthLayout>;
}

const emailSchema = z.object({ email: z.email('Enter a valid email') });
export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false); const { toast } = useToast();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ email: string }>({ resolver: zodResolver(emailSchema) });
  const submit = async ({ email }: { email: string }) => { try { await authApi.forgotPassword(email); setSent(true); } catch (error) { toast('Request failed', { kind: 'error', description: getApiMessage(error) }); } };
  return <AuthLayout kicker="Account recovery" title={sent ? 'Check your inbox' : 'Reset your password'} description={sent ? 'If an account exists for that email, a secure reset link is on its way.' : 'Enter your work email and we’ll send a secure reset link.'}>{sent ? <div className="rounded-2xl border border-brand-200 bg-brand-50 p-6 text-center dark:border-brand-900 dark:bg-brand-950/50"><div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-brand-100 text-brand-700"><Mail className="h-5 w-5" /></div><p className="mt-4 text-sm text-ink-600 dark:text-ink-300">The link expires soon for your security.</p></div> : <form onSubmit={handleSubmit(submit)} className="space-y-5"><Field label="Work email" error={errors.email?.message}><Input autoFocus type="email" autoComplete="email" {...register('email')} /></Field><Button type="submit" className="w-full" size="lg" disabled={isSubmitting}>{isSubmitting && <LoaderCircle className="h-4 w-4 animate-spin" />}Send reset link</Button></form>}<Link to="/login" className="mt-7 flex items-center justify-center gap-2 text-sm font-semibold text-ink-600 hover:text-ink-900 dark:text-ink-300"><ArrowLeft className="h-4 w-4" />Back to sign in</Link></AuthLayout>;
}

export function ResendVerificationPage() {
  const [params] = useSearchParams();
  const [sentTo, setSentTo] = useState<string | null>(null);
  const { toast } = useToast();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ email: string }>({
    resolver: zodResolver(emailSchema),
    defaultValues: { email: params.get('email') ?? '' },
  });

  const submit = async ({ email }: { email: string }) => {
    try {
      await authApi.resendVerification(email);
      setSentTo(email);
    } catch (error) {
      toast('Could not request a new link', { kind: 'error', description: getApiMessage(error) });
    }
  };

  return <AuthLayout kicker="Email verification" title={sentTo ? 'Check your inbox' : 'Request a fresh link'} description={sentTo ? 'For your security, we show the same result whether or not an account exists.' : 'Enter the email used for your workspace and we’ll send a new verification link if the account is eligible.'}>{sentTo ? <div className="rounded-2xl border border-brand-200 bg-brand-50 p-6 text-center dark:border-brand-900 dark:bg-brand-950/50"><div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-brand-100 text-brand-700"><Mail className="h-5 w-5" /></div><p className="mt-4 text-sm leading-6 text-ink-600 dark:text-ink-300">If a pending account exists for <span className="font-semibold text-ink-900 dark:text-white">{sentTo}</span>, a new link is on its way.</p><Button variant="secondary" className="mt-5" onClick={() => setSentTo(null)}>Use another email</Button></div> : <form onSubmit={handleSubmit(submit)} className="space-y-5" noValidate><Field label="Work email" error={errors.email?.message}><Input autoFocus type="email" autoComplete="email" placeholder="you@company.com" invalid={Boolean(errors.email)} {...register('email')} /></Field><Button type="submit" className="w-full" size="lg" disabled={isSubmitting}>{isSubmitting && <LoaderCircle className="h-4 w-4 animate-spin" />}Send verification link</Button></form>}<Link to="/login" className="mt-7 flex items-center justify-center gap-2 text-sm font-semibold text-ink-600 hover:text-ink-900 dark:text-ink-300"><ArrowLeft className="h-4 w-4" />Back to sign in</Link></AuthLayout>;
}

const resetSchema = z.object({ password: z.string().min(10, 'Use at least 10 characters').regex(/[a-z]/, 'Include a lowercase letter').regex(/[A-Z]/, 'Include an uppercase letter').regex(/[0-9]/, 'Include a number'), confirmPassword: z.string() }).refine((v) => v.password === v.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' });
export function ResetPasswordPage() {
  const token = useOneTimeToken(); const navigate = useNavigate(); const { toast } = useToast();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<z.infer<typeof resetSchema>>({ resolver: zodResolver(resetSchema) });
  const submit = async ({ password }: z.infer<typeof resetSchema>) => { if (!token) return toast('Invalid reset link', { kind: 'error' }); try { await authApi.resetPassword(token, password); toast('Password updated'); navigate('/login'); } catch (error) { toast('Could not reset password', { kind: 'error', description: getApiMessage(error) }); } };
  return <AuthLayout kicker="Set a new password" title="Secure your account" description="Choose a strong password you have not used before."><form onSubmit={handleSubmit(submit)} className="space-y-5"><Field label="New password" error={errors.password?.message}><Input autoFocus type="password" autoComplete="new-password" {...register('password')} /></Field><Field label="Confirm password" error={errors.confirmPassword?.message}><Input type="password" autoComplete="new-password" {...register('confirmPassword')} /></Field><Button type="submit" className="w-full" size="lg" disabled={isSubmitting}>{isSubmitting && <LoaderCircle className="h-4 w-4 animate-spin" />}Update password</Button></form></AuthLayout>;
}

export function VerifyEmailPage() {
  const token = useOneTimeToken(); const [state, setState] = useState<'loading' | 'success' | 'error'>('loading');
  useEffect(() => { if (!token) { setState('error'); return; } authApi.verifyEmail(token).then(() => setState('success')).catch(() => setState('error')); }, [token]);
  return <AuthLayout kicker="Email verification" title={state === 'loading' ? 'Verifying your email' : state === 'success' ? 'You’re verified' : 'That link is not valid'} description={state === 'loading' ? 'This takes just a moment.' : state === 'success' ? 'Your workspace is ready when you are.' : 'The verification link may have expired or already been used.'}><div className="text-center">{state === 'loading' ? <LoaderCircle className="mx-auto h-10 w-10 animate-spin text-brand-600" /> : state === 'success' ? <div className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-brand-100 text-brand-700"><Check className="h-7 w-7" /></div> : <div className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-rose-100 text-rose-700"><ShieldCheck className="h-7 w-7" /></div>}<div className="mt-7 flex flex-col justify-center gap-3 sm:flex-row">{state === 'error' && <Link to="/resend-verification" className="inline-flex h-11 items-center justify-center rounded-lg bg-gradient-to-r from-brand-600 to-sky-700 px-4 text-sm font-semibold text-white shadow-sm transition hover:from-brand-700 hover:to-sky-800">Request a new link</Link>}<Link to="/login" className="inline-flex h-11 items-center justify-center rounded-lg border border-ink-200 bg-white px-4 text-sm font-semibold text-ink-800 shadow-sm transition hover:bg-ink-50 dark:border-ink-700 dark:bg-ink-900 dark:text-ink-100 dark:hover:bg-ink-800">{state === 'success' ? 'Continue to sign in' : 'Back to sign in'}</Link></div></div></AuthLayout>;
}
